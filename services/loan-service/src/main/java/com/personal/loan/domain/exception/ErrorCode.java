package com.personal.loan.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    ACCESS_DENIED(2000, "Access denied.", HttpStatus.FORBIDDEN),
    INVALID_ARGUMENTS(1004, "Invalid arguments.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BOOK_STOCK(1301, "Not enough copies available", HttpStatus.CONFLICT),
    LOAN_NOT_FOUND(1302, "Loan not found.", HttpStatus.NOT_FOUND),
    LOAN_ALREADY_RETURNED(1303, "Loan is already returned.", HttpStatus.CONFLICT),
    BOOK_NOT_FOUND(1304, "Book is not found.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(1305, "User is not found.", HttpStatus.NOT_FOUND),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}
