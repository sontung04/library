package com.personal.user.services;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAuthCacheService {

    private static final String USER_AUTH_KEY_PREFIX = "auth:user:";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_ROLES = "roles";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public UserAuthSnapshot loadUserAuthSnapshot(Long userId, Duration ttl) {
        String key = USER_AUTH_KEY_PREFIX + userId;

        List<Object> cached = redisTemplate.opsForHash().multiGet(
                key,
                List.of(FIELD_USERNAME, FIELD_ROLES));

        if (cached != null && cached.size() == 2
                && cached.get(0) != null
                && cached.get(1) != null) {
            String username = cached.get(0).toString();
            String rolesCsv = cached.get(1).toString();
            List<String> roles = Arrays.stream(rolesCsv.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isBlank())
                    .toList();
            return new UserAuthSnapshot(userId, username, roles);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        UserAuthSnapshot snapshot = fromUser(user);
        cache(snapshot, ttl);
        return snapshot;
    }

    public void cache(User user, Duration ttl) {
        cache(fromUser(user), ttl);
    }

    public void evict(Long userId) {
        redisTemplate.delete(USER_AUTH_KEY_PREFIX + userId);
    }

    public Optional<UserAuthSnapshot> readFromCache(Long userId) {
        String key = USER_AUTH_KEY_PREFIX + userId;
        List<Object> cached = redisTemplate.opsForHash().multiGet(key, List.of(FIELD_USERNAME, FIELD_ROLES));
        if (cached == null || cached.size() != 2 || cached.get(0) == null || cached.get(1) == null) {
            return Optional.empty();
        }

        String username = cached.get(0).toString();
        List<String> roles = Arrays.stream(cached.get(1).toString().split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();

        return Optional.of(new UserAuthSnapshot(userId, username, roles));
    }

    private UserAuthSnapshot fromUser(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::name)
                .toList();
        return new UserAuthSnapshot(user.getId(), user.getUsername(), roles);
    }

    private void cache(UserAuthSnapshot snapshot, Duration ttl) {
        String key = USER_AUTH_KEY_PREFIX + snapshot.userId();
        redisTemplate.opsForHash().put(key, FIELD_USERNAME, snapshot.username());
        redisTemplate.opsForHash().put(key, FIELD_ROLES, String.join(",", snapshot.roles()));
        redisTemplate.expire(key, ttl);
    }

    public record UserAuthSnapshot(Long userId, String username, List<String> roles) {
    }
}
