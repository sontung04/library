package com.personal.user.dtos;

import java.util.Arrays;
import java.util.List;

import com.personal.user.entities.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDto {

    private final Long id;
    private final String username;
    private final String email;
    private final List<String> roles;

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(), 
                user.getUsername(), 
                user.getEmail(), 
                Arrays.asList(user.getRoles().toString()));
    }
}
