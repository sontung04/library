package com.personal.loan.api.dtos;

import java.time.LocalDate;

import com.personal.loan.domain.entities.LoanStatus;

public record LoanDto(
    Long id,
    Long userId,
    String userUsername,
    boolean userDeleted,
    LoanItemDto item,
    LocalDate loanDate,
    LocalDate dueDate,
    LocalDate returnDate,
    LoanStatus status
) {}
