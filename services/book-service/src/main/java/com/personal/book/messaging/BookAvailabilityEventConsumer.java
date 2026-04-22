package com.personal.book.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.book.domain.services.BookAvailabilityEventHandler;
import com.personal.book.domain.services.BookService;
import com.personal.book.events.BookAvailabilityEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BookAvailabilityEventConsumer {

    private final ObjectMapper objectMapper;
    private final BookAvailabilityEventHandler bookService;
    private static final String TOPIC = "book-availability";

    public BookAvailabilityEventConsumer(ObjectMapper objectMapper, BookService bookService) {
        this.objectMapper = objectMapper;
        this.bookService = bookService;
    }

    /**
     * Consumes book availability events
     * 
     * @param payload
     */
    @KafkaListener(topics = TOPIC)
    public void consume(String payload) {
        try {
            BookAvailabilityEvent event = objectMapper.readValue(payload, BookAvailabilityEvent.class);
            switch (event.action()) {
                case LOAN:
                    bookService.handleLoanEvent(event.eventId(), event.bookId());
                    break;
                case RETURN:
                    bookService.handleReturnEvent(event.eventId(), event.bookId());
                    break;
                default:
                    log.error("Cannot recognize book availability action.");
                    break;
            }
            log.info("Book availability event consumed.");
        } catch (Exception ex) {
            log.error("Failed to consume book-availability event: {}, error: {}", payload, ex);
        }
    }
}
