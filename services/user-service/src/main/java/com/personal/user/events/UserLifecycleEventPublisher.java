package com.personal.user.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.user.dtos.KafkaUserEventPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka's publisher that pushes User-related modification events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLifecycleEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "user-lifecycle";

    /**
     * Publish user-lifecycle events 
     * 
     * @param user User payload that other services need in order to process.
     */
    public void publish(KafkaUserEventPayload payload, Action userAction) {
        try {
            String message = objectMapper.writeValueAsString(
                    new UserLifecycleEvent(payload, userAction));

            kafkaTemplate.send(
                TOPIC, 
                String.valueOf(payload.id()), 
                message);
            
            log.info("User {} event has been sent with id = {}", userAction.name(), payload.id());
        } catch (Exception ex) {
            log.error("Failed to publish user-lifecycle event for userId={}", payload.id(), ex);
        }
    }
}
