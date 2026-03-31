package com.personal.loan.domain.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.api.mappers.LoanMapper;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanItem;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.repositories.LoanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookClient bookClient;

    @Transactional
    public LoanDto createLoan(Long userId, CreateLoanRequest request) {
        log.info("Executing LoanService::createLoan");

        List<LoanItemDto> items = List.of(new LoanItemDto(request.bookId(), 1, null));

        if (!checkBooksStatus(items))
            throw new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK);

        items.forEach(this::subtractBookAmountFromDb);

        Loan createdLoan = loanRepository.save(LoanMapper.toEntity(userId, request));

        return enrichLoan(createdLoan);
    }

    private boolean checkBooksStatus(List<LoanItemDto> items) {
        return items.stream().allMatch(item -> {
            BookDto book = bookClient.getBook(item.bookId());
            return book != null && book.availableCopies() >= item.amount();
        });
    }

    private void subtractBookAmountFromDb(LoanItemDto item) {
        BookDto book = bookClient.getBook(item.bookId());
        int newAvailable = book.availableCopies() - item.amount();
        bookClient.updateAvailability(item.bookId(), newAvailable);
    }

    public List<LoanDto> getUserLoans(Long userId) {
        List<Loan> userLoans = retrieveUserLoansFromDb(userId);

        if (userLoans.isEmpty())
            return Collections.emptyList();

        return userLoans.stream()
                .map(this::enrichLoan)
                .toList();
    }

    public List<LoanDto> getLoansByUserId(Long userId) {
        return getUserLoans(userId);
    }

    private List<Loan> retrieveUserLoansFromDb(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    public List<LoanDto> getAllLoans() {
        List<Loan> loans = retrieveAllLoansFromDb();

        if (loans.isEmpty())
            return Collections.emptyList();

        return loans.stream()
                .map(this::enrichLoan)
                .toList();
    }

    private List<Loan> retrieveAllLoansFromDb() {
        return loanRepository.findAll();
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        log.info("Executing LoanService::returnLoan");

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new WebException(ErrorCode.LOAN_NOT_FOUND));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new WebException(ErrorCode.LOAN_NOT_ACTIVE);
        }

        loan.getItems().forEach(this::restoreBookStock);
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        return enrichLoan(loanRepository.save(loan));
    }

    private LoanDto enrichLoan(Loan loan) {
        LoanDto baseLoan = LoanMapper.toDto(loan);
        List<LoanItemDto> enrichedItems = baseLoan.items().stream()
                .map(item -> new LoanItemDto(item.bookId(), item.amount(), bookClient.getBook(item.bookId())))
                .toList();

        return new LoanDto(
                baseLoan.id(),
                baseLoan.userId(),
                enrichedItems,
                baseLoan.loanDate(),
                baseLoan.dueDate(),
                baseLoan.returnDate(),
                baseLoan.status());
    }

    private void restoreBookStock(LoanItem item) {
        BookDto book = bookClient.getBook(item.getBookId());
        if (book != null) {
            bookClient.updateAvailability(item.getBookId(), book.availableCopies() + item.getAmount());
        }
    }
}
