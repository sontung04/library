package com.personal.loan.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.personal.loan.api.dtos.ApiResponse;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.domain.services.LoanService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoanDto> createLoan(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateLoanRequest request) {
        return new ApiResponse<>("Loan created.", loanService.createLoan(userId, request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LoanDto>> getMyLoans(
            @RequestHeader("X-User-Id") Long userId) {
        return new ApiResponse<>(loanService.getUserLoans(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LoanDto>> getAllLoans() {
        return new ApiResponse<>(loanService.getAllLoans());
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<LoanDto>> getLoansByUserId(@PathVariable Long userId) {
        return new ApiResponse<>(loanService.getUserLoans(userId));
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LoanDto> returnLoan(
            @PathVariable Long id) {
        return new ApiResponse<>("Loan returned.", loanService.returnLoan(id));
    }
}
