package com.personal.book.idempotency;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "processed_events")
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /** The eventId carried in the incoming Kafka message. */
    @Id
    @Column(nullable = false, length = 64)
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
