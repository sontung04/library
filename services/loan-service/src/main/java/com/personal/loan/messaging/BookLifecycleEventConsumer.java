package com.personal.loan.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.loan.domain.services.BookService;
import com.personal.loan.events.BookLifecycleEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka's consumer that consumes book-related modification events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookLifecycleEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookService bookService;

    /**
     * Consumes book lifecycle events
     * 
     * @param payload
     */
    @KafkaListener(topics = "book-lifecycle")
    public void consume(String payload) {
        try {
            BookLifecycleEvent event = objectMapper.readValue(payload, BookLifecycleEvent.class);
            switch (event.bookAction()) {
                case DELETE:
                    bookService.handleDeletionEvent(event.payload().id());
                    break;
                case CREATE:
                    bookService.handleCreationEvent(event.payload());
                    break;
                case UPDATE:
                    bookService.handleUpdateEvent(event.payload());
                    break;
                default:
                    log.error("Cannot recognize book action.");
                    break;
            }

            log.info("Book lifecycle event consumed.");
        } catch (Exception ex) {
            log.error("Failed to consume book-lifecycle event: {}", payload, ex);
        }
    }
}
