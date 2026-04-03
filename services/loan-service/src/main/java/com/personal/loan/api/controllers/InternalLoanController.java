package com.personal.loan.api.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.loan.api.dtos.ActiveLoanCheckResponse;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.services.LoanService;

@RestController
@RequestMapping("/internal/loans")
public class InternalLoanController {

    private final LoanService loanService;
    private final String internalApiKey;

    public InternalLoanController(
            LoanService loanService,
            @Value("${app.internal.api-key:library-internal-key}") String internalApiKey) {
        this.loanService = loanService;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/users/{userId}/active-exists")
    public ActiveLoanCheckResponse hasActiveLoans(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String requestApiKey) {
        if (requestApiKey == null || !requestApiKey.equals(internalApiKey)) {
            throw new WebException(ErrorCode.ACCESS_DENIED);
        }
        return new ActiveLoanCheckResponse(loanService.hasActiveLoans(userId));
    }
}
