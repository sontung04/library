package com.personal.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.personal.user.dtos.AuthResponse;
import com.personal.user.dtos.LoginRequest;
import com.personal.user.dtos.RegisterRequest;
import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.properties.JwtProperties;
import com.personal.user.repositories.UserRepository;
import com.personal.user.services.JwtService.TokenBundle;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @Mock
    private TokenStateService tokenStateService;

    @InjectMocks
    private AuthenticationService authenticationService;

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_ok_savesUser() throws WebException {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret");

        User saved = new User();
        saved.setId(1L);
        saved.setUsername("alice");
        saved.setEmail("alice@example.com");
        saved.setPasswordHash("hashed");
        saved.setRoles(Arrays.asList(Role.ROLE_USER));

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authenticationService.register(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_missingField_throwsNullPointerException() {
        // The service does no null-guard; validation is the controller's
        // responsibility.
        assertThrows(NullPointerException.class, () -> authenticationService.register(null));
    }

    @Test
    void register_usernameAlreadyExists_throwsWebException() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        WebException ex = assertThrows(WebException.class, () -> authenticationService.register(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_EXISTS);
        verify(userRepository, never()).save(any());
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_ok_returnsAuthResponse() throws WebException {
        LoginRequest request = new LoginRequest("alice", "secret");

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        user.setRoles(Arrays.asList(Role.ROLE_USER));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtProperties.userCacheTtlSeconds()).thenReturn(3600L);
        when(jwtService.mintAccessToken(anyString(), anyList()))
                .thenReturn(new TokenBundle("mock.jwt.access", "jti-access", Duration.ofSeconds(3600L)));
        when(jwtService.mintRefreshToken(anyString()))
                .thenReturn(new TokenBundle("mock.jwt.refresh", "jti-refresh", Duration.ofSeconds(86400L)));

        AuthResponse response = authenticationService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("mock.jwt.access");
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void login_missingField_throwsNullPointerException() {
        // The service does no null-guard; validation is the controller's
        // responsibility.
        assertThrows(NullPointerException.class, () -> authenticationService.login(null));
    }

    @Test
    void login_usernameNotFound_throwsWebException() {
        LoginRequest request = new LoginRequest("ghost", "secret");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        WebException ex = assertThrows(WebException.class, () -> authenticationService.login(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void login_passwordNotMatch_throwsWebException() {
        LoginRequest request = new LoginRequest("alice", "wrongpass");

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hashed");
        user.setRoles(Arrays.asList(Role.ROLE_USER));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        WebException ex = assertThrows(WebException.class, () -> authenticationService.login(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
