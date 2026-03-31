package com.personal.user.exceptions;

import java.util.Objects;

import lombok.Getter;

@Getter
public class WebException extends RuntimeException {

    private final ErrorCode errorCode;

    public WebException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").getErrorMessage());
        this.errorCode = errorCode;
    }
}
