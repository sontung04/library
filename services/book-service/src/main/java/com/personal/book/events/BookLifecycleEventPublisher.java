package com.personal.book.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.book.domain.entities.Book;

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

    /**
     * Publishes messages on book deletion.
     * 
     * @param book book that has been deleted
     */
    public void publishDeleted(Book book) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new BookLifecycleEvent(book.getId(), book.getTitle(), book.getIsbn(), true));
            kafkaTemplate.send("book-lifecycle", String.valueOf(book.getId()), payload);
        } catch (Exception ex) {
            log.error("Failed to publish book-lifecycle event for bookId={}", book.getId(), ex);
        }
    }
}
