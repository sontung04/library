package com.personal.user.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.user.entities.User;

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

    /**
     * Publish messages on user update
     * 
     * @param user User info that other services need to update.
     */
    public void publishUpdated(User user) {
        publish(user, false);
    }

    /**
     * Publish messages on user deletion
     * 
     * @param user User info that other services need to update.
     */
    public void publishDeleted(User user) {
        publish(user, true);
    }

    private void publish(User user, boolean deleted) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new UserLifecycleEvent(user.getId(), user.getUsername(), deleted));
            kafkaTemplate.send("user-lifecycle", String.valueOf(user.getId()), payload);
        } catch (Exception ex) {
            log.error("Failed to publish user-lifecycle event for userId={}", user.getId(), ex);
        }
    }
}
