package com.personal.user.services;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.personal.user.dtos.CreateUserRequest;
import com.personal.user.dtos.UpdateUserRequest;
import com.personal.user.dtos.UserDto;
import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.repositories.UserRepository;
import com.personal.user.events.UserLifecycleEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthCacheService userAuthCacheService;
    private final TokenStateService tokenStateService;
    private final LoanClient loanClient;
    private final UserLifecycleEventPublisher userLifecycleEventPublisher;

    /**
     * Fetches a {@link User} by ID from the repository and logs the result.
     *
     * @param userId the ID of the user to look up
     * @return the found {@link User} entity
     * @throws WebException with {@link ErrorCode#USER_NOT_FOUND} if no user exists
     *                      for the given ID
     */
    private User findUserAndLog(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        log.info("User found! Username: {}, email: {}",
                user.getUsername(),
                user.getEmail());
        return user;
    }

    /**
     * Returns the public profile of a single user.
     *
     * @param userId the ID of the user to retrieve
     * @return a {@link UserDto} representing the user's public data
     * @throws WebException with {@link ErrorCode#USER_NOT_FOUND} if the user does
     *                      not exist
     */
    public UserDto getUserInfo(Long userId) {
        log.info("Execute UserService:getUserInfo");

        User user = findUserAndLog(userId);
        return UserDto.fromEntity(user);
    }

    /**
     * Creates a new user account from an admin-supplied request.
     * The password is BCrypt-hashed before persistence.
     * If {@code request.roles()} is {@code null}, the account defaults to
     * {@link Role#ROLE_USER}.
     *
     * @param request the creation payload containing username, email, password, and
     *                optional roles
     * @return a {@link UserDto} representing the newly created user
     */
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating a user.");
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles() == null ? Arrays.asList(Role.ROLE_USER) : request.roles());

        User newUser = userRepository.save(user);
        log.info("A new user has been created. User id: {}, username: {}, email: {}",
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail());
        return UserDto.fromEntity(newUser);
    }

    /**
     * Updates an existing user's profile and, optionally, their roles.
     *
     * <p>
     * Business rules enforced:
     * <ul>
     * <li>An admin cannot remove {@link Role#ROLE_ADMIN} from their own account
     * ({@link ErrorCode#CANNOT_DEMOTE_SELF}).</li>
     * <li>If {@code request.roles()} is {@code null} the user's existing roles are
     * preserved;
     * the field is never silently defaulted to {@code ROLE_USER}.</li>
     * <li>When roles actually change the target user's active refresh token family
     * is
     * invalidated via {@link TokenStateService#clearActiveRefreshJti()}, forcing a
     * re-login so the new roles take effect immediately.</li>
     * <li>Every role change is written to the application log as an audit
     * entry.</li>
     * </ul>
     *
     * @param userId         the ID of the user to update
     * @param request        the update payload (username, email, optional password,
     *                       optional roles)
     * @param currentAdminId the ID of the admin performing the update, injected
     *                       from the
     *                       {@code X-User-Id} gateway header
     * @return a {@link UserDto} reflecting the updated state
     * @throws WebException with {@link ErrorCode#USER_NOT_FOUND} if the target user
     *                      does not exist
     * @throws WebException with {@link ErrorCode#CANNOT_DEMOTE_SELF} if the admin
     *                      tries to remove
     *                      their own admin role
     */
    public UserDto updateUser(Long userId, UpdateUserRequest request, Long currentAdminId) {
        log.info("Updating a user with id = {}", userId);
        User user = findUserAndLog(userId);

        // Prevent an admin from removing their own ROLE_ADMIN
        if (currentAdminId.equals(userId)
                && request.roles() != null
                && !request.roles().contains(Role.ROLE_ADMIN)) {
            throw new WebException(ErrorCode.CANNOT_DEMOTE_SELF);
        }

        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.password() != null)
            user.setPasswordHash(passwordEncoder.encode(request.password()));

        List<Role> oldRoles = user.getRoles();
        // Keep existing roles when the caller omits the roles field
        List<Role> newRoles = request.roles() == null ? oldRoles : request.roles();
        user.setRoles(newRoles);

        User updatedUser = userRepository.save(user);
        userAuthCacheService.evict(updatedUser.getId());
        userLifecycleEventPublisher.publishUpdated(updatedUser);

        if (!newRoles.equals(oldRoles)) {
            log.info("AUDIT: Admin {} changed roles of user {} from {} to {}",
                    currentAdminId, userId, oldRoles, newRoles);
            // Force the target user to re-authenticate with their new roles
            tokenStateService.clearActiveRefreshJti(userId);
        }

        log.info("A user has been updated. User id: {}, username: {}, email: {}",
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail());
        return UserDto.fromEntity(updatedUser);
    }

    /**
     * Permanently deletes a user account.
     *
     * <p>
     * The operation is refused if the user still has active loans, preventing
     * orphaned
     * loan records. On success, the following cleanup is performed:
     * <ul>
     * <li>The user row is removed from Postgres.</li>
     * <li>The auth snapshot is evicted from Redis.</li>
     * <li>The active refresh-token JTI family is cleared from Redis.</li>
     * <li>A {@code UserDeletedEvent} is published to Kafka for downstream
     * consumers.</li>
     * </ul>
     *
     * @param userId the ID of the user to delete
     * @throws WebException with {@link ErrorCode#USER_NOT_FOUND} if the user does
     *                      not exist
     * @throws WebException with {@link ErrorCode#USER_HAS_ACTIVE_LOANS} if the user
     *                      has unreturned books
     */
    public void deleteUser(Long userId) {
        log.info("Deleting a specified user if exists");

        User user = findUserAndLog(userId);
        if (loanClient.hasActiveLoans(userId)) {
            throw new WebException(ErrorCode.USER_HAS_ACTIVE_LOANS);
        }
        userRepository.delete(user);
        userAuthCacheService.evict(userId);
        tokenStateService.clearActiveRefreshJti(userId);
        userLifecycleEventPublisher.publishDeleted(user);
    }

    /**
     * Returns the profiles of all registered users.
     *
     * @return a list of {@link UserDto} for every user in the database; empty if
     *         none exist
     */
    public List<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(UserDto::fromEntity)
                .toList();
    }
}
