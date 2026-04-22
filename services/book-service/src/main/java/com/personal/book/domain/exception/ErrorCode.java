package com.personal.book.domain.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    ACCESS_DENIED(2000, "Access denied.", HttpStatus.FORBIDDEN),
    INVALID_ARGUMENTS(1004, "Invalid arguments.", HttpStatus.BAD_REQUEST),
    BOOK_NOT_FOUND(1201, "Book not found.", HttpStatus.NOT_FOUND),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception.", HttpStatus.INTERNAL_SERVER_ERROR), 
    OUT_OF_STOCK(1202, "Book out of stock.", HttpStatus.CONFLICT);

    private final int code;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}