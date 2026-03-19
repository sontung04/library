package com.personal.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.user.dtos.ApiResponse;
import com.personal.user.dtos.CreateUserRequest;
import com.personal.user.dtos.UpdateUserRequest;
import com.personal.user.dtos.UserDto;
import com.personal.user.services.UserService;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }
    
    @PostMapping("/new")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody CreateUserRequest request) {
        ApiResponse<UserDto> response = new ApiResponse<>(
            1000, 
            "user_created", 
            userService.createUser(request));
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/update")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest request) {
        ApiResponse<UserDto> response = new ApiResponse<>(
            1000, 
            "user_updated", 
            userService.updateUser(userId, request));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<ApiResponse<UserDto>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);

        return ResponseEntity.noContent().build();
    }
}
