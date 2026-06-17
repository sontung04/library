package com.personal.loan.api.dtos;

import java.time.LocalDate;

import com.personal.loan.domain.entities.LoanStatus;

public record LoanDto(
    Long id,
    UserDto user,
    LoanItemDto item,
    LocalDate loanDate,
    LocalDate dueDate,
    LocalDate returnDate,
    LoanStatus status
) {}
