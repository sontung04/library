package com.personal.book.api.dtos;

import jakarta.validation.constraints.Min;

public record IncreaseBookCopiesRequest(
        @Min(1) int additionalCopies) {
}