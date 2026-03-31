package com.personal.book.api.dtos;

public record UpdateBookRequest(
    String title,
    String author,
    String category,
    String isbn,
    Integer totalCopies
) {}
