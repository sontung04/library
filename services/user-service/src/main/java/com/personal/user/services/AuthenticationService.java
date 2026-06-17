package com.personal.user.services;

import java.util.Arrays;
import java.time.Duration;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.user.dtos.AuthResponse;
import com.personal.user.dtos.LoginRequest;
import com.personal.user.dtos.LogoutRequest;
import com.personal.user.dtos.RefreshTokenRequest;
import com.personal.user.dtos.RegisterRequest;
import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.properties.JwtProperties;
import com.personal.user.repositories.UserRepository;
import com.personal.user.services.JwtService.TokenBundle;

import io.jsonwebtoken.Claims;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserAuthCacheService userAuthCacheService;
    private final TokenStateService tokenStateService;
    private static final String BEARER_TOKEN_TYPE = "Bearer ";

    @Transactional
    public void register(RegisterRequest request) throws WebException {
        log.info("Execute AuthenticationService::register");

        // Check if user exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("Error while registering: Username {} exists", request.getUsername());
            throw new WebException(ErrorCode.USER_EXISTS);
        }

        createNewUser(request);
        log.info("Complete executing AuthenticationService::register");
    }

    private void createNewUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoles(Arrays.asList(Role.ROLE_USER));

        User newUser = userRepository.save(user);
        log.info("A new user is created. User id: {}, username: {}, email: {}",
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) throws WebException {
        log.info("Execute AuthenticationService::login");

        User user = userRepository.findByUsername(request.username().toLowerCase())
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        log.info("Checking credentials...");
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new WebException(ErrorCode.INVALID_CREDENTIALS);
        }

        userAuthCacheService.cache(user, Duration.ofSeconds(jwtProperties.userCacheTtlSeconds()));
        AuthResponse response = generateAuthResponse(user);

        log.info("Complete executing AuthenticationService::login");
        return response;
    }

    private AuthResponse generateAuthResponse(User user) {
        log.info("Generating token response for username {}", user.getUsername());

        List<String> roles = user.getRoles()
                .stream()
                .map(Role::name)
                .toList();

        TokenBundle access = jwtService.mintAccessToken(user.getId().toString(), roles);
        TokenBundle refresh = jwtService.mintRefreshToken(user.getId().toString());
        tokenStateService.setActiveRefreshJti(user.getId(), refresh.jti(), refresh.ttl());

        log.info("Token pair generated for userId={}", user.getId());

        return new AuthResponse(
                access.token(),
                refresh.token(),
                BEARER_TOKEN_TYPE,
                access.ttl().getSeconds(),
                refresh.ttl().getSeconds(),
                user.getId(),
                user.getUsername(),
                roles);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        Long userId = jwtService.extractSubjectWithoutValidation(request.refreshToken());
        UserAuthCacheService.UserAuthSnapshot snapshot = userAuthCacheService.loadUserAuthSnapshot(
                userId,
                Duration.ofSeconds(jwtProperties.userCacheTtlSeconds()));

        Claims claims = jwtService.validateToken(request.refreshToken(), JwtService.TOKEN_TYPE_REFRESH);
        String currentJti = claims.getId();

        if (currentJti == null || tokenStateService.isJtiRevoked(currentJti)) {
            throw new WebException(ErrorCode.TOKEN_REVOKED);
        }

        String activeJti = tokenStateService.getActiveRefreshJti(userId)
                .orElseThrow(() -> new WebException(ErrorCode.UNAUTHORIZED));

        if (!activeJti.equals(currentJti)) {
            // Token theft suspected: a second party used the same refresh token family.
            // Revoke the active session so neither party can continue.
            tokenStateService.revokeJti(activeJti, Duration.ofSeconds(jwtProperties.refreshExpirationSeconds()));
            tokenStateService.clearActiveRefreshJti(userId);
            log.warn("SECURITY: Refresh token reuse detected for userId={}. Active session revoked.", userId);
            throw new WebException(ErrorCode.TOKEN_REVOKED);
        }

        tokenStateService.revokeJti(currentJti, Duration.ofSeconds(jwtProperties.refreshExpirationSeconds()));

        TokenBundle newAccess = jwtService.mintAccessToken(userId.toString(), snapshot.roles());
        TokenBundle newRefresh = jwtService.mintRefreshToken(userId.toString());
        tokenStateService.setActiveRefreshJti(userId, newRefresh.jti(), newRefresh.ttl());

        return new AuthResponse(
                newAccess.token(),
                newRefresh.token(),
                BEARER_TOKEN_TYPE,
                newAccess.ttl().getSeconds(),
                newRefresh.ttl().getSeconds(),
                snapshot.userId(),
                snapshot.username(),
                snapshot.roles());
    }

    public void logout(LogoutRequest request, String authorizationHeader) {
        Long userId = jwtService.extractSubjectWithoutValidation(request.refreshToken());
        userAuthCacheService.loadUserAuthSnapshot(
                userId,
                Duration.ofSeconds(jwtProperties.userCacheTtlSeconds()));

        Claims refreshClaims = jwtService.validateToken(request.refreshToken(), JwtService.TOKEN_TYPE_REFRESH);
        String refreshJti = refreshClaims.getId();

        if (refreshJti != null) {
            tokenStateService.revokeJti(refreshJti, Duration.ofSeconds(jwtProperties.refreshExpirationSeconds()));
        }
        tokenStateService.clearActiveRefreshJti(userId);

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_TOKEN_TYPE)) {
            String accessToken = authorizationHeader.substring(7);
            Claims accessClaims = jwtService.validateToken(accessToken, JwtService.TOKEN_TYPE_ACCESS);
            String accessJti = accessClaims.getId();
            if (accessJti != null) {
                tokenStateService.revokeJti(accessJti, Duration.ofSeconds(jwtProperties.accessExpirationSeconds()));
            }
        }
    }
}
