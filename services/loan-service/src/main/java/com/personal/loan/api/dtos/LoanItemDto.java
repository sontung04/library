package com.personal.loan.api.dtos;

public record LoanItemDto(
    Long bookId,
    String bookTitle,
    String bookIsbn
) {}
