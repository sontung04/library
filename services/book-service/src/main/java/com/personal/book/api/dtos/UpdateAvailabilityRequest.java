package com.personal.book.api.dtos;

import jakarta.validation.constraints.Min;

public record UpdateAvailabilityRequest(
    @Min(0)
    int availableCopies
) {}
