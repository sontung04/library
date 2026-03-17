package com.personal.user.Dtos;

import java.util.List;

public record AuthResponse(

    String token,
    String tokenType,
    Long expiresIn, // in seconds

    Long userId,
    String username,
    List<String> roles
) {}
