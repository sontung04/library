package com.personal.user.events;

import com.personal.user.dtos.KafkaUserEventPayload;

public record UserLifecycleEvent(
    KafkaUserEventPayload payload,
    Action userAction) {
}
