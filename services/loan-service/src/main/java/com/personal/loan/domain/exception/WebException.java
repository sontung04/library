package com.personal.loan.domain.exception;

import lombok.Getter;

@Getter
public class WebException extends RuntimeException  {

    private final ErrorCode errorCode;
    
    public WebException(ErrorCode errorCode) {
        super(errorCode.getErrorMessage());
        this.errorCode = errorCode;
    }
}
