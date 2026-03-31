package com.personal.loan.api.dtos;

public record LoanItemDto(
    Long bookId,
    Integer amount,
    BookDto book
) {
    
}
