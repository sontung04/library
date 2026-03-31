package com.personal.loan.api.dtos;

public record BookDto(
    Long id,
    String title,
    String author,
    String category,
    String isbn,
    int availableCopies,
    int totalCopies
) {}
