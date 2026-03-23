package com.personal.book.api.dtos;

import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(

    @NotBlank
    String title,

    @NotBlank
    String author,

    String category,

    @NotBlank
    String isbn,

    @NotBlank
    Integer copies
) {}
