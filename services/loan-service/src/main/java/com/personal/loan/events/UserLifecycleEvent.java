package com.personal.loan.events;

import com.personal.loan.api.dtos.KafkaUserEventPayload;

public record UserLifecycleEvent(
        KafkaUserEventPayload payload,
        UserAction userAction) {
}
