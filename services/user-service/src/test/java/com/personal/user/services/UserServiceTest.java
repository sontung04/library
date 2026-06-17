package com.personal.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.user.dtos.CreateUserRequest;
import com.personal.user.dtos.UpdateUserRequest;
import com.personal.user.dtos.UserDto;
import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.outbox.KafkaOutboxEvent;
import com.personal.user.outbox.KafkaOutboxEventRepository;
import com.personal.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserAuthCacheService userAuthCacheService;

    @Mock
    private TokenStateService tokenStateService;

    @Mock
    private LoanClient loanClient;

    @Mock
    private KafkaOutboxEventRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_withoutRoles_defaultsToRoleUser() {
        CreateUserRequest request = new CreateUserRequest("alice", "alice@example.com", "secret", null);

        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserDto dto = userService.createUser(request);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getRoles()).containsExactly(Role.ROLE_USER);
    }

    @Test
    void updateUser_withPassword_evictsCacheAndPublishesEvent() {
        UpdateUserRequest request = new UpdateUserRequest("alice2", "alice2@example.com", "new-secret",
                List.of(Role.ROLE_LIBRARIAN));

        User existing = new User(1L, "alice", "alice@example.com", "old-hash", List.of(Role.ROLE_USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDto dto = userService.updateUser(1L, request, 99L);

        assertThat(dto.getUsername()).isEqualTo("alice2");
        assertThat(dto.getRoles()).containsExactly(Role.ROLE_LIBRARIAN);
        verify(userAuthCacheService).evict(1L);
        verify(outboxRepository).save(any(KafkaOutboxEvent.class));
        verify(tokenStateService).clearActiveRefreshJti(1L);
    }

    @Test
    void deleteUser_withActiveLoans_throwsConflictAndDoesNotDelete() {
        User existing = new User(1L, "alice", "alice@example.com", "hash", List.of(Role.ROLE_USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(loanClient.hasActiveLoans(1L)).thenReturn(true);

        WebException ex = assertThrows(WebException.class, () -> userService.deleteUser(1L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_HAS_ACTIVE_LOANS);
        verify(userRepository, never()).delete(any(User.class));
        verify(outboxRepository, never()).save(any(KafkaOutboxEvent.class));
    }

    @Test
    void deleteUser_withoutActiveLoans_deletesAndClearsState() {
        User existing = new User(1L, "alice", "alice@example.com", "hash", List.of(Role.ROLE_USER));
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(loanClient.hasActiveLoans(1L)).thenReturn(false);

        userService.deleteUser(1L);

        verify(userRepository).delete(existing);
        verify(userAuthCacheService).evict(1L);
        verify(tokenStateService).clearActiveRefreshJti(1L);
        verify(outboxRepository).save(any(KafkaOutboxEvent.class));
    }

    @Test
    void getUserInfo_whenMissing_throwsUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        WebException ex = assertThrows(WebException.class, () -> userService.getUserInfo(999L));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}
