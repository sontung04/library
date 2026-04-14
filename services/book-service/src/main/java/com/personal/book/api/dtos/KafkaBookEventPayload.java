package com.personal.book.api.dtos;

public record KafkaBookEventPayload(
        Long id,
        String title,
        String isbn,
        int availableCopies) {
}