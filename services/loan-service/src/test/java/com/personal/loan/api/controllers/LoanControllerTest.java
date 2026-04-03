package com.personal.loan.api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.GlobalExceptionHandler;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.services.LoanService;

@ExtendWith(MockitoExtension.class)
class LoanControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LoanService loanService;

    @InjectMocks
    private LoanController loanController;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createLoan_ok_returnsCreated() throws Exception {
        LoanDto dto = new LoanDto(
                11L,
                7L,
                "alice",
                false,
                new LoanItemDto(5L, "Clean Code", "ISBN-1", false),
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null,
                LoanStatus.ACTIVE);
        when(loanService.createLoan(any(Long.class), any(CreateLoanRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/loans")
                .header("X-User-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateLoanRequest(5L, LocalDate.now().plusDays(10)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Loan created."))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.item.bookId").value(5));
    }

    @Test
    void createLoan_invalidBookId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/loans")
                .header("X-User-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bookId\":0,\"dueDate\":\"2030-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENTS.getCode()));
    }

    @Test
    void createLoan_whenNoStock_returnsConflict() throws Exception {
        when(loanService.createLoan(any(Long.class), any(CreateLoanRequest.class)))
                .thenThrow(new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK));

        mockMvc.perform(post("/api/loans")
                .header("X-User-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateLoanRequest(5L, LocalDate.now().plusDays(10)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.INSUFFICIENT_BOOK_STOCK.getCode()));
    }

    @Test
    void getMyLoans_ok_returnsLoanList() throws Exception {
        List<LoanDto> loans = List.of(
                new LoanDto(11L, 7L, "alice", false, new LoanItemDto(5L, "Clean Code", "ISBN-1", false),
                        LocalDate.now().minusDays(2), LocalDate.now().plusDays(10), null, LoanStatus.ACTIVE));
        when(loanService.getUserLoans(7L)).thenReturn(loans);

        mockMvc.perform(get("/api/loans/my").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].item.bookTitle").value("Clean Code"));
    }

    @Test
    void returnLoan_ok_returnsLoanReturnedMessage() throws Exception {
        LoanDto returned = new LoanDto(
                11L,
                7L,
                "alice",
                false,
                new LoanItemDto(5L, "Clean Code", "ISBN-1", false),
                LocalDate.now().minusDays(5),
                LocalDate.now().minusDays(1),
                LocalDate.now(),
                LoanStatus.RETURNED);
        when(loanService.returnLoan(11L)).thenReturn(returned);

        mockMvc.perform(patch("/api/loans/11/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Loan returned."))
                .andExpect(jsonPath("$.data.status").value("RETURNED"));
    }
}
