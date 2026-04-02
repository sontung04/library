package com.personal.user.services;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenStateService {

    private static final String ACTIVE_REFRESH_KEY_PREFIX = "auth:refresh:active:";
    private static final String REVOKED_JTI_KEY_PREFIX = "auth:jti:revoked:";

    private final StringRedisTemplate redisTemplate;

    public void setActiveRefreshJti(Long userId, String jti, Duration ttl) {
        redisTemplate.opsForValue().set(ACTIVE_REFRESH_KEY_PREFIX + userId, jti, ttl);
    }

    public Optional<String> getActiveRefreshJti(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(ACTIVE_REFRESH_KEY_PREFIX + userId));
    }

    public void clearActiveRefreshJti(Long userId) {
        redisTemplate.delete(ACTIVE_REFRESH_KEY_PREFIX + userId);
    }

    public void revokeJti(String jti, Duration ttl) {
        redisTemplate.opsForValue().set(REVOKED_JTI_KEY_PREFIX + jti, "1", ttl);
    }

    public boolean isJtiRevoked(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_JTI_KEY_PREFIX + jti));
    }
}
