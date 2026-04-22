package com.personal.book.events;

import com.personal.book.api.dtos.KafkaBookEventPayload;

public record BookLifecycleEvent(
    KafkaBookEventPayload payload,
    LifecycleAction bookAction) {
}
