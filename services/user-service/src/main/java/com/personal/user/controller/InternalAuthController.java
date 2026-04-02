package com.personal.user.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.user.dtos.InternalUserAuthSnapshotResponse;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.properties.JwtProperties;
import com.personal.user.services.UserAuthCacheService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserAuthCacheService userAuthCacheService;
    private final JwtProperties jwtProperties;

    @Value("${app.internal.api-key:library-internal-key}")
    private String internalApiKey;

    @GetMapping("/users/{id}")
    public InternalUserAuthSnapshotResponse getUserAuthSnapshot(
            @PathVariable("id") Long userId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String requestApiKey) {

        if (requestApiKey == null || !requestApiKey.equals(internalApiKey)) {
            throw new WebException(ErrorCode.UNAUTHORIZED);
        }

        UserAuthCacheService.UserAuthSnapshot snapshot = userAuthCacheService.loadUserAuthSnapshot(
                userId,
                Duration.ofSeconds(jwtProperties.userCacheTtlSeconds()));

        return new InternalUserAuthSnapshotResponse(snapshot.userId(), snapshot.username(), snapshot.roles());
    }
}
