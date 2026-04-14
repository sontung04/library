package com.personal.book.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.book.api.dtos.KafkaBookEventPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka's publisher that pushes book-related modification events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookLifecycleEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "book-lifecycle";

    /**
     * Publishes messages on book events.
     * 
     * @param payload book info
     */
    public void publish(KafkaBookEventPayload payload, Action bookAction) {
        try {
            String message = objectMapper.writeValueAsString(
                    new BookLifecycleEvent(payload, bookAction));
            kafkaTemplate.send(
                TOPIC, 
                String.valueOf(payload.id()), 
                message);
            
            log.info("Book {} event has been sent with book id = {}", bookAction.name(), payload.id());
        } catch (Exception ex) {
            log.error("Failed to publish book-lifecycle event for bookId={}", payload.id(), ex);
        }
    }
}
