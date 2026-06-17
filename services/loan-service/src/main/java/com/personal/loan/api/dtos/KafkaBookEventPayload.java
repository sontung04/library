package com.personal.loan.api.dtos;

public record KafkaBookEventPayload(
        Long id,
        String title,
        String isbn,
        int availableCopies) {
}
