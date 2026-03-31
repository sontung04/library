package com.personal.book.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookRequest(

    @NotBlank
    String title,

    @NotBlank
    String author,

    String category,

    @NotBlank
    String isbn,

    @NotNull
    Integer copies
) {}
