package com.personal.loan.api.dtos;

public record KafkaUserEventPayload(
    Long id,
    String username,
    String email
) {}
