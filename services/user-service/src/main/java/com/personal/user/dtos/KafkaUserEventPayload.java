package com.personal.user.dtos;

public record KafkaUserEventPayload (
    Long id,
    String username,
    String email 
) {}
