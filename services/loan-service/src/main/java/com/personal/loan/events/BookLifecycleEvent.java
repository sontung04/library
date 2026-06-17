package com.personal.loan.events;

import com.personal.loan.api.dtos.KafkaBookEventPayload;

public record BookLifecycleEvent(
        KafkaBookEventPayload payload,
        BookAction bookAction) {
}
