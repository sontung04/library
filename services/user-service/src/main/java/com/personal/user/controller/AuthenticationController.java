package com.personal.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import com.personal.user.dtos.ApiResponse;
import com.personal.user.dtos.AuthResponse;
import com.personal.user.dtos.LoginRequest;
import com.personal.user.dtos.LogoutRequest;
import com.personal.user.dtos.RefreshTokenRequest;
import com.personal.user.dtos.RegisterRequest;
import com.personal.user.services.AuthenticationService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Running endpoint /auth/register");

        authenticationService.register(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Running endpoint /auth/login");

        return ResponseEntity.ok(
                new ApiResponse<AuthResponse>(
                        1000,
                        "successful",
                        authenticationService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Running endpoint /auth/refresh");

        return ResponseEntity.ok(
                new ApiResponse<>(
                        1000,
                        "successful",
                        authenticationService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        log.info("Running endpoint /auth/logout");

        authenticationService.logout(request, authorizationHeader);
        return ResponseEntity.noContent().build();
    }
}
