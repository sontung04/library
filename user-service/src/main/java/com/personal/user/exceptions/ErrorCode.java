package com.personal.user.exceptions;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    
    USER_EXISTS(1001, "User exists.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "User not found.", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1003, "Invalid credentials.", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String errorMessage, HttpStatus httpStatus) {
        this.code = code;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }
    int code;
    String errorMessage;
    HttpStatus httpStatus;
}
