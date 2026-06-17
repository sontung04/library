package com.personal.user.utils;

import com.personal.user.dtos.KafkaUserEventPayload;
import com.personal.user.entities.User;

public class UserMapper {

    private UserMapper() {
    }

    public static KafkaUserEventPayload toKafkaPayload(User user) {
        return new KafkaUserEventPayload(
                user.getId(),
                user.getUsername(),
                user.getEmail());
    }
}
