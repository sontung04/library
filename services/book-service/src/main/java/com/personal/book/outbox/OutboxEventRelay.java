package com.personal.book.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRelay {

    private final KafkaOutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:5000}")
    @Transactional
    public void relay() {
        var pending = outboxRepository.findByPublishedFalseOrderByCreatedAtAsc();
        for (KafkaOutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getKey(), event.getPayload()).get();
                event.setPublished(true);
                outboxRepository.save(event);
                log.debug("Relayed outbox event id={} topic={}", event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to relay outbox event id={}, will retry next cycle", event.getId(), e);
            }
        }
    }
}
