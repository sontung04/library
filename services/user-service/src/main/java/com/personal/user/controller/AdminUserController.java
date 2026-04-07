package com.personal.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.personal.user.dtos.ApiResponse;
import com.personal.user.dtos.CreateUserRequest;
import com.personal.user.dtos.UpdateUserRequest;
import com.personal.user.dtos.UserDto;
import com.personal.user.services.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Null;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<UserDto>> getAllUsers() {
        log.info("Running endpoint GET /api/admin/users");
        log.info("Retrieving all users.");

        return new ApiResponse<>(
                1000,
                "Retrieved all users",
                userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserDto> getUserInfo(@PathVariable Long id) {
        log.info("Running endpoint GET /api/admin/users/{}", id);

        return new ApiResponse<>(
                1000,
                String.format("User %d info retrieved", id),
                userService.getUserInfo(id));
    }

    @PostMapping("/new")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Running endpoint POST /api/admin/users");

        return new ApiResponse<>(
                1000,
                "user_created",
                userService.createUser(request));
    }

    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            @RequestHeader("X-User-Id") Long currentAdminId) {
        log.info("Running endpoint PUT /api/admin/users/{}", userId);

        return new ApiResponse<>(
                1000,
                "user_updated",
                userService.updateUser(userId, request, currentAdminId));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Null> deleteUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long currentAdminId) {
        log.info("Running endpoint DELETE /api/admin/users/{}/delete", userId);

        userService.deleteUser(userId);

        return new ApiResponse<>(
                1000,
                "user_deleted",
                null);
    }
}
