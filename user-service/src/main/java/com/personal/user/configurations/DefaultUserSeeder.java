package com.personal.user.configurations;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultUserSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        createIfMissing("default-librarian", "default-librarian@example.com", "123456", Role.ROLE_LIBRARIAN);
        createIfMissing("default-admin", "default-admin@example.com", "123456", Role.ROLE_ADMIN);
    }

    private void createIfMissing(String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            log.info("Default user {} already exists, skipping seeding", username);
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoles(List.of(role));

        userRepository.save(user);
        log.info("Seeded default user {} with role {}", username, role);
    }
}
