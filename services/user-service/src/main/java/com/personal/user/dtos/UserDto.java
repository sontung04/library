package com.personal.user.dtos;

import java.util.ArrayList;
import java.util.List;

import com.personal.user.entities.User;
import com.personal.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDto {

    private final Long id;
    private final String username;
    private final String email;
    private final List<Role> roles;

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(), 
                user.getUsername(), 
                user.getEmail(), 
                new ArrayList<>(user.getRoles()));
    }
}
