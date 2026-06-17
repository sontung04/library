package com.personal.gateway.dto;

import java.util.List;

public record GatewayUserAuthSnapshot(
        Long userId,
        String username,
        List<String> roles) {
}
