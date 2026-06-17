package com.personal.user.dtos;

import java.util.List;

import com.personal.user.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
    @NotBlank
    String username,

    @NotBlank
    @Email
    String email,

    String password,

    List<Role> roles
) {}
