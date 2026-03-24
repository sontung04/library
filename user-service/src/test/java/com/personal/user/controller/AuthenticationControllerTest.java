package com.personal.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.user.configurations.SecurityConfig;
import com.personal.user.dtos.AuthResponse;
import com.personal.user.dtos.LoginRequest;
import com.personal.user.dtos.RegisterRequest;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.services.AuthenticationService;

import org.springframework.context.annotation.Import;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfig.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    // ─── POST /api/users/register ─────────────────────────────────────────────

    @Test
    void register_ok_returns204() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret");

        doNothing().when(authenticationService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void register_missingField_returns400() throws Exception {
        // Bean Validation triggers MethodArgumentNotValidException
        String body = """
                {"username": "", "email": "alice@example.com", "password": "secret"}
                """;

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENTS.getCode()));
    }

    @Test
    void register_usernameExists_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret");

        doThrow(new WebException(ErrorCode.USER_EXISTS))
                .when(authenticationService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_EXISTS.getErrorMessage()));
    }

    // ─── POST /api/users/login ────────────────────────────────────────────────

    @Test
    void login_ok_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("alice", "secret");

        AuthResponse authResponse = new AuthResponse(
                "mock.jwt.token",
                "Bearer ",
                3600L,
                1L,
                "alice",
                List.of("ROLE_USER"));

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("successful"))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void login_missingField_returns400() throws Exception {
        // Bean Validation triggers MethodArgumentNotValidException
        String body = """
                {"username": "alice", "password": ""}
                """;

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENTS.getCode()));
    }

    @Test
    void login_usernameNotFound_returns400() throws Exception {
        LoginRequest request = new LoginRequest("ghost", "secret");

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new WebException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getErrorMessage()));
    }

    @Test
    void login_passwordNotMatch_returns400() throws Exception {
        LoginRequest request = new LoginRequest("alice", "wrongpass");

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new WebException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getErrorMessage()));
    }
}
