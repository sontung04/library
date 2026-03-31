package com.personal.loan.api.dtos;

import java.time.LocalDate;
import java.util.List;

import com.personal.loan.domain.entities.LoanStatus;

public record LoanDto(
    Long id,
    Long userId,
    List<LoanItemDto> items,
    LocalDate loanDate,
    LocalDate dueDate,
    LocalDate returnDate,
    LoanStatus status
) {}
