package com.personal.loan.api.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
    
    @NotNull
    @Min(1)
    Long bookId,

    LocalDate dueDate
) {}
