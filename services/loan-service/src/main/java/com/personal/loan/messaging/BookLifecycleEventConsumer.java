package com.personal.loan.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.loan.domain.services.LoanService;
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
    private final LoanService loanService;

    /**
     * Consumes book deletion events
     * 
     * @param payload
     */
    @KafkaListener(topics = "book-lifecycle")
    public void consume(String payload) {
        try {
            BookLifecycleEvent event = objectMapper.readValue(payload, BookLifecycleEvent.class);
            if (event.deleted()) {
                loanService.applyBookDeletedEvent(event.bookId(), event.title(), event.isbn());
            }
        } catch (Exception ex) {
            log.error("Failed to consume book-lifecycle event: {}", payload, ex);
        }
    }
}
