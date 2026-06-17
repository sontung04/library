package com.personal.loan.api.mappers;

import com.personal.loan.api.dtos.KafkaUserEventPayload;
import com.personal.loan.api.dtos.UserDto;
import com.personal.loan.domain.entities.User;

public class UserMapper {

    private UserMapper() {}

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername());
    }

    public static User toEntity(KafkaUserEventPayload payload) {
        return new User(
            payload.id(),
            payload.username(),
            payload.email()
        );
    }
}
