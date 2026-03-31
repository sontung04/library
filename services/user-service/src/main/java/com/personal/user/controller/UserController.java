package com.personal.user.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.user.dtos.ApiResponse;
import com.personal.user.dtos.UserDto;
import com.personal.user.exceptions.ErrorCode;
import com.personal.user.exceptions.WebException;
import com.personal.user.services.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getUserInfo(
            @RequestHeader("X-User-Id") String userId
    ) {
        try {
            Long userIdInNumber = Long.valueOf(userId);

            ApiResponse<UserDto> response = new ApiResponse<>(
                    1000,
                    "User found.",
                    userService.getUserInfo(userIdInNumber));
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("Failed parsing userId to Long.");
            throw new WebException(ErrorCode.INVALID_ARGUMENTS);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(new ApiResponse<>(1000, "Users found.", userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(1000, "User found.", userService.getUserInfo(id)));
    }
}
