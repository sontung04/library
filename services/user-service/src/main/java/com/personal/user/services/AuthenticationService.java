package com.personal.user.services;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.user.dtos.AuthResponse;
import com.personal.user.dtos.LoginRequest;
import com.personal.user.dtos.RegisterRequest;
import com.personal.user.entities.User;
import com.personal.user.enums.Role;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
            newUser.getEmail()
        );
    }

    public AuthResponse login(LoginRequest request) throws WebException {
        log.info("Execute AuthenticationService::login");

        User user = userRepository.findByUsername(request.username().toLowerCase())
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));
        
        log.info("Checking credentials...");
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new WebException(ErrorCode.INVALID_CREDENTIALS);
        }

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

        String token = jwtService.mintToken(user.getId().toString(), roles);

        log.info("Token generated: {} ", token);

        return new AuthResponse(
            token,
            "Bearer ",
            3600L,
            user.getId(),
            user.getUsername(),
            roles
        );
    }

    public void logout(Long id) {
        
    }
}
