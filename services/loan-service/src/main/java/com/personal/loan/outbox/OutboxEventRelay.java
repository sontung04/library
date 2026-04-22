package com.personal.loan.outbox;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Polls the {@code kafka_outbox_events} table for unpublished entries and
 * forwards them to Kafka. Runs in its own transaction so that marking an event
 * as published is atomic with the Kafka send acknowledgement.
 *
 * <p>
 * Because events are only written to the outbox inside a business transaction,
 * a rolled-back loan or return will never leave a dangling outbox row, and
 * Kafka
 * will therefore never receive a spurious message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private final KafkaOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relay() {
        List<KafkaOutboxEvent> pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox relay: {} pending event(s) to publish.", pending.size());

        for (KafkaOutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload()).get();
                event.setPublished(true);
                outboxRepository.save(event);
                log.info("Outbox relay: published event id={} to topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Outbox relay: failed to publish event id={}, will retry next cycle.", event.getId(), e);
                // Do not rethrow — let the loop continue for other events.
                // This event will be retried on the next scheduled run.
            }
        }
    }
}
