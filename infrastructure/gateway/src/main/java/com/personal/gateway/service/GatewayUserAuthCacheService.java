package com.personal.gateway.service;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import com.personal.gateway.dto.GatewayUserAuthSnapshot;

import reactor.core.publisher.Mono;

@Service
public class GatewayUserAuthCacheService {

    private static final String USER_AUTH_KEY_PREFIX = "auth:user:";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_ROLES = "roles";

    private final ReactiveStringRedisTemplate redisTemplate;

    public GatewayUserAuthCacheService(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<GatewayUserAuthSnapshot> readSnapshot(Long userId) {
        String key = USER_AUTH_KEY_PREFIX + userId;

        return redisTemplate.opsForHash()
                .multiGet(key, java.util.List.of(FIELD_USERNAME, FIELD_ROLES))
                .flatMap(values -> {
                    if (values == null || values.size() != 2 || values.get(0) == null || values.get(1) == null) {
                        return Mono.empty();
                    }

                    String username = values.get(0).toString();
                    String rolesCsv = values.get(1).toString();

                    return Mono.just(new GatewayUserAuthSnapshot(
                            userId,
                            username,
                            Arrays.stream(rolesCsv.split(","))
                                    .map(String::trim)
                                    .filter(role -> !role.isBlank())
                                    .toList()));
                });
    }

    public Mono<Void> cacheSnapshot(GatewayUserAuthSnapshot snapshot, Duration ttl) {
        String key = USER_AUTH_KEY_PREFIX + snapshot.userId();

        return redisTemplate.opsForHash()
                .put(key, FIELD_USERNAME, snapshot.username())
                .then(redisTemplate.opsForHash().put(key, FIELD_ROLES, String.join(",", snapshot.roles())))
                .then(redisTemplate.expire(key, ttl))
                .then();
    }

    public Mono<Boolean> isJtiRevoked(String jti) {
        return redisTemplate.hasKey("auth:jti:revoked:" + jti);
    }
}
