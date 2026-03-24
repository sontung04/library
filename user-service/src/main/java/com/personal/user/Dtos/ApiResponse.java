package com.personal.user.dtos;

public record ApiResponse<T>(
    int code,
    String message,
    T data
) {
    
}
