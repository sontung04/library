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



    private User findUserAndLog(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        log.info("User found! Username: {}, email: {}", 
                user.getUsername(), 
                user.getEmail());
        return user;
    }
    
    public UserDto getUserInfo(Long userId) {
        log.info("Execute UserService:getUserInfo");
            
        User user = findUserAndLog(userId);
        return UserDto.fromEntity(user);
    }

    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating a user.");
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles() == null ? 
                Arrays.asList(Role.ROLE_USER) :
                request.roles());

        User newUser = userRepository.save(user);
        log.info("A new user has been created. User id: {}, username: {}, email: {}", 
            newUser.getId(), 
            newUser.getUsername(), 
            newUser.getEmail()
        );
        return UserDto.fromEntity(newUser);
    }

    public UserDto updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating a user with id = {}", userId);
        User user = findUserAndLog(userId);
        user.setUsername(request.username());
        user.setEmail(request.email());
        if (request.password() != null)
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles() == null ? 
                Arrays.asList(Role.ROLE_USER) :
                request.roles());
        
        User updatedUser = userRepository.save(user);
        userAuthCacheService.evict(updatedUser.getId());
        log.info("A user has been updated. User id: {}, username: {}, email: {}", 
            updatedUser.getId(), 
            updatedUser.getUsername(), 
            updatedUser.getEmail()
        );
        return UserDto.fromEntity(updatedUser);
    }

    public void deleteUser(Long userId) {
        log.info("Deleting a specified user if exists");
        userRepository.deleteById(userId);
        userAuthCacheService.evict(userId);
        tokenStateService.clearActiveRefreshJti(userId);
    }

    public List<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(UserDto::fromEntity)
                .toList();
    }
}
