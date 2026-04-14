package com.personal.loan.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.loan.domain.services.UserService;
import com.personal.loan.events.UserLifecycleEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka's consumer that consumes user-related modification events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLifecycleEventConsumer {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    /**
     * Consumes user-lifecycle events
     * 
     * @param payload <code>UserLifecycleEvent</code> message
     */
    @KafkaListener(topics = "user-lifecycle")
    public void consume(String payload) {
        try {
            UserLifecycleEvent event = objectMapper.readValue(payload, UserLifecycleEvent.class);
            switch (event.userAction()) {
                case CREATE:
                    userService.handleCreationEvent(event.payload());
                    break;
                case UPDATE:
                    userService.handleUpdateEvent(event.payload());
                    break;
                case DELETE:
                    userService.handleDeletionEvent(event.payload().id());
                    break;
                default:
                    log.error("Cannot recognize user action");
                    break;
            }
            log.info("User event consumed.");
        } catch (Exception ex) {
            log.error("Failed to consume user-lifecycle event: {}", payload, ex);
        }
    }
}
