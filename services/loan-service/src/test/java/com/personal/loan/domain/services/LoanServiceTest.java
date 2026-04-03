package com.personal.loan.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.repositories.LoanRepository;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookClient bookClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private LoanService loanService;

    @Test
    void createLoan_success_savesLoanAndUpdatesStock() {
        Long userId = 7L;
        CreateLoanRequest request = new CreateLoanRequest(5L, LocalDate.now().plusDays(10));
        BookDto book = new BookDto(5L, "Clean Code", "Robert C. Martin", "Tech", "ISBN-1", 3, 3);

        when(bookClient.getBook(5L)).thenReturn(book);
        when(bookClient.updateAvailability(5L, 2)).thenReturn(true);
        when(userClient.getUsername(userId)).thenReturn("alice");
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan loan = invocation.getArgument(0);
            loan.setId(11L);
            return loan;
        });

        LoanDto result = loanService.createLoan(userId, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.item()).isNotNull();
        assertThat(result.item().bookId()).isEqualTo(5L);
        verify(bookClient).updateAvailability(5L, 2);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void createLoan_noStock_throwsInsufficientBookStock() {
        CreateLoanRequest request = new CreateLoanRequest(5L, LocalDate.now().plusDays(10));
        BookDto book = new BookDto(5L, "Clean Code", "Robert C. Martin", "Tech", "ISBN-1", 0, 3);
        when(bookClient.getBook(5L)).thenReturn(book);

        WebException ex = assertThrows(WebException.class, () -> loanService.createLoan(7L, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_BOOK_STOCK);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void createLoan_whenStockUpdateFails_throwsUncategorizedException() {
        CreateLoanRequest request = new CreateLoanRequest(5L, LocalDate.now().plusDays(10));
        BookDto book = new BookDto(5L, "Clean Code", "Robert C. Martin", "Tech", "ISBN-1", 1, 3);

        when(bookClient.getBook(5L)).thenReturn(book);
        when(bookClient.updateAvailability(5L, 0)).thenReturn(false);

        WebException ex = assertThrows(WebException.class, () -> loanService.createLoan(7L, request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNCATEGORIZED_EXCEPTION);
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void returnLoan_activeLoan_marksReturnedAndRestoresStock() {
        Loan loan = new Loan(11L, 7L, "alice", false, 5L, "Clean Code", "ISBN-1", false,
                LoanStatus.ACTIVE, LocalDate.now().minusDays(2), LocalDate.now().plusDays(10), null);

        when(loanRepository.findById(11L)).thenReturn(Optional.of(loan));
        when(bookClient.getBook(5L))
                .thenReturn(new BookDto(5L, "Clean Code", "Robert C. Martin", "Tech", "ISBN-1", 2, 3));
        when(bookClient.updateAvailability(5L, 3)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDto result = loanService.returnLoan(11L);

        assertThat(result.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(result.returnDate()).isNotNull();
        verify(bookClient).updateAvailability(5L, 3);
    }

    @Test
    void applyUserLifecycleEvent_updatesAllLoansForUser() {
        Loan first = new Loan(1L, 7L, "old-name", false, 5L, "Book", "ISBN", false,
                LoanStatus.ACTIVE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(14), null);
        Loan second = new Loan(2L, 7L, "old-name", false, 8L, "Book2", "ISBN2", false,
                LoanStatus.RETURNED, LocalDate.now().minusDays(4), LocalDate.now().plusDays(10), LocalDate.now());
        when(loanRepository.findByUserId(7L)).thenReturn(List.of(first, second));

        loanService.applyUserLifecycleEvent(7L, "new-name", true);

        ArgumentCaptor<List<Loan>> captor = ArgumentCaptor.forClass(List.class);
        verify(loanRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).allSatisfy(loan -> {
            assertThat(loan.getUserUsername()).isEqualTo("new-name");
            assertThat(loan.isUserDeleted()).isTrue();
        });
    }
}
