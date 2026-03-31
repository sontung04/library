package com.personal.book.api.dtos;

public record ApiResponse<T>(
        int code,
        String message,
        T data) {
    private static final int DEFAULT_CODE = 200;
    private static final String DEFAULT_MESSAGE = "Success";

    public ApiResponse(T data) {
        this(DEFAULT_CODE, DEFAULT_MESSAGE, data);
    }

    public ApiResponse(int code, String message) {
        this(code, message, null);
    }

    public ApiResponse() {
        this(DEFAULT_CODE, DEFAULT_MESSAGE, null);
    }

    public ApiResponse(String message, T data) {
        this(DEFAULT_CODE, message, data);
    }
}
