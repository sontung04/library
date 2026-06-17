package com.personal.loan.api.dtos;

public record BookDto(
        Long id,
        String title,
        String isbn,
        int availableCopies) {
}
