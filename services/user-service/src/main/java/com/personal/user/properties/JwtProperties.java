package com.personal.user.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long accessExpirationSeconds,
        long refreshExpirationSeconds,
        long userCacheTtlSeconds) {
}