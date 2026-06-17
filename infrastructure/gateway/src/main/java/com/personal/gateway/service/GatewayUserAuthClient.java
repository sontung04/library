package com.personal.gateway.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.personal.gateway.dto.GatewayUserAuthSnapshot;

import reactor.core.publisher.Mono;

@Service
public class GatewayUserAuthClient {

    private final WebClient webClient;
    private final GatewayUserAuthCacheService cacheService;

    public GatewayUserAuthClient(WebClient webClient, GatewayUserAuthCacheService cacheService) {
        this.webClient = webClient;
        this.cacheService = cacheService;
    }

    @Value("${app.internal.api-key}")
    private String internalApiKey;

    @Value("${app.auth.user-cache-ttl-seconds:3600}")
    private long userCacheTtlSeconds;

    public Mono<GatewayUserAuthSnapshot> fetchAndCache(Long userId) {
        return webClient.get()
                .uri("http://user-service/internal/auth/users/{id}", userId)
                .header("X-Internal-Api-Key", internalApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(GatewayUserAuthSnapshot.class)
                .flatMap(snapshot -> cacheService.cacheSnapshot(snapshot, Duration.ofSeconds(userCacheTtlSeconds))
                        .thenReturn(snapshot));
    }
}
