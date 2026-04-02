package com.personal.user.dtos;

import java.util.List;

public record AuthResponse(

    String accessToken,
    String refreshToken,
    String tokenType,
    Long accessExpiresIn,
    Long refreshExpiresIn,

    Long userId,
    String username,
    List<String> roles
) {}
