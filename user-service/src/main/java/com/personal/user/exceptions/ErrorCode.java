package com.personal.user.exceptions;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    USER_EXISTS(1001, "User exists.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1002, "User not found.", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1003, "Invalid credentials.", HttpStatus.BAD_REQUEST),
    INVALID_ARGUMENTS(1004, "Invalid arguments", HttpStatus.BAD_REQUEST),

    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception.", HttpStatus.INTERNAL_SERVER_ERROR);
    

    private final int code;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}
