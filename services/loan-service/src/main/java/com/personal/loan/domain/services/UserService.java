package com.personal.loan.domain.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.loan.api.dtos.KafkaUserEventPayload;
import com.personal.loan.api.dtos.UserDto;
import com.personal.loan.api.mappers.UserMapper;
import com.personal.loan.domain.entities.User;
import com.personal.loan.domain.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements LifecycleEventHandler<Long, KafkaUserEventPayload> {

    private final UserRepository userRepository;
    private final LoanService loanService;

    /**
     * Applies a user deletion event to all of the user's loan snapshot records.
     * Triggered by a Kafka {@code UserLifecycleEvent} with <code>DELETE</code>
     * action
     * consumed from user-service.
     *
     * @param userId the ID of the affected user
     */
    @Override
    @Transactional
    public void handleDeletionEvent(Long userId) {

        if (!userRepository.existsById(userId)) {
            log.warn("User {} already deleted, skipping duplicate DELETE event.", userId);
            return;
        }
        loanService.deleteUserId(userId);
        userRepository.deleteById(userId);
        logAction(userId, Action.DELETE);
    }

    @Override
    @Transactional
    public void handleCreationEvent(KafkaUserEventPayload payload) {

        Action userAction = Action.CREATE;
        User user = UserMapper.toEntity(payload);

        Long userId = userRepository.save(user).getId();
        logAction(userId, userAction);
    }

    @Override
    @Transactional
    public void handleUpdateEvent(KafkaUserEventPayload payload) {

        Action userAction = Action.UPDATE;
        Optional<User> user = userRepository.findById(payload.id());
        User userEntity;

        // Create a new user if user is not found in db
        if (user.isEmpty()) {
            log.info("User with id = {} cannot be found in database. Creating a new user.", payload.id());
            userAction = Action.CREATE;
            userEntity = UserMapper.toEntity(payload);
        } else {
            userEntity = user.get();
            userEntity.setUsername(payload.username());
            userEntity.setEmail(payload.email());
        }

        Long savedUserId = userRepository.save(userEntity).getId();
        logAction(savedUserId, userAction);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            log.error("Cannot find user with id = {}", userId);
            return null;
        } else
            return UserMapper.toDto(user.get());
    }

    private void logAction(Long userId, Action userAction) {
        log.info("User {} with id = {}", userAction.label(), userId);
    }
}
