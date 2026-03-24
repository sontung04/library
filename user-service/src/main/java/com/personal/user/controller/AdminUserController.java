package com.personal.user.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.personal.user.dtos.ApiResponse;
import com.personal.user.dtos.CreateUserRequest;
import com.personal.user.dtos.UpdateUserRequest;
import com.personal.user.dtos.UserDto;
import com.personal.user.services.UserService;

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
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        log.info("Running endpoint GET /api/admin/users");
        log.info("Retrieving all users.");

        ApiResponse<List<UserDto>> response = new ApiResponse<>(
            1000, 
            "Retrieved all users", 
            userService.getAllUsers());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}") 
    public ResponseEntity<ApiResponse<UserDto>> getUserInfo(@PathVariable Long id) {
        log.info("Running endpoint GET /api/admin/users/{}", id);

        ApiResponse<UserDto> response = new ApiResponse<>(
            1000,
            String.format("User %d info retrieved", id),
            userService.getUserInfo(id));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/new")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody CreateUserRequest request) {
        log.info("Running endpoint POST /api/admin/users");

        ApiResponse<UserDto> response = new ApiResponse<>(
            1000, 
            "user_created", 
            userService.createUser(request));
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        log.info("Running endpoint PUT /api/admin/users/{}", userId);
        ApiResponse<UserDto> response = new ApiResponse<>(
            1000, 
            "user_updated", 
            userService.updateUser(userId, request));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> deleteUser(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long currentAdminId) {
        log.info("Running endpoint DELETE /api/admin/users/{}/delete", userId);

        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
