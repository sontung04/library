package com.personal.loan.domain.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.api.mappers.LoanMapper;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.repositories.LoanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookClient bookClient;
    private final UserClient userClient;

    @Transactional
    public LoanDto createLoan(Long userId, CreateLoanRequest request) {
        log.info("Executing LoanService::createLoan");

        BookDto book = bookClient.getBook(request.bookId());

        if (!checkBooksAvailability(book))
            throw new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK);

        LoanItemDto item = new LoanItemDto(
                request.bookId(),
                book.title(),
                book.isbn(),
                false);

        subtractBookAmountFromDb(item, book);

        String username = userClient.getUsername(userId);
        Loan createdLoan = loanRepository.save(LoanMapper.toEntity(userId, username, request, book));
        log.info("Loan created successfully: loanId={}, userId={}, bookId={}", createdLoan.getId(), userId,
                request.bookId());

        return LoanMapper.toDto(createdLoan);
    }

    private boolean checkBooksAvailability(BookDto book) {
        if (book == null)
            return false;
        return book.availableCopies() > 0;
    }

    private void subtractBookAmountFromDb(LoanItemDto item, BookDto currentBook) {
        int newAvailableCopies = currentBook.availableCopies() - 1;
        boolean stockUpdated = bookClient.updateAvailability(item.bookId(), newAvailableCopies);
        if (!stockUpdated) {
            throw new WebException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Transactional(readOnly = true)
    public List<LoanDto> getUserLoans(Long userId) {
        List<Loan> userLoans = loanRepository.findByUserId(userId);

        if (userLoans.isEmpty())
            return Collections.emptyList();

        return userLoans.stream()
                .map(LoanMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveLoans(Long userId) {
        return loanRepository.existsByUserIdAndStatus(userId, LoanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<LoanDto> getAllLoans() {
        List<Loan> loans = loanRepository.findAll();

        if (loans.isEmpty())
            return Collections.emptyList();

        return loans.stream()
                .map(LoanMapper::toDto)
                .toList();
    }

    @Transactional
    public LoanDto returnLoan(Long loanId) {
        log.info("Executing LoanService::returnLoan");

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new WebException(ErrorCode.LOAN_NOT_FOUND));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new WebException(ErrorCode.LOAN_NOT_ACTIVE);
        }

        restoreBookStock(loan.getBookId());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        return LoanMapper.toDto(loanRepository.save(loan));
    }

    @Transactional
    public void applyUserLifecycleEvent(Long userId, String username, boolean deleted) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        loans.forEach(loan -> {
            loan.setUserUsername(username);
            loan.setUserDeleted(deleted);
        });
        loanRepository.saveAll(loans);
    }

    @Transactional
    public void applyBookDeletedEvent(Long bookId, String title, String isbn) {
        List<Loan> loans = loanRepository.findByBookId(bookId);
        loans.forEach(loan -> {
            loan.setBookTitle(title);
            loan.setBookIsbn(isbn);
            loan.setBookDeleted(true);
        });
        loanRepository.saveAll(loans);
    }

    private void restoreBookStock(Long bookId) {
        BookDto book = bookClient.getBook(bookId);
        if (book != null) {
            boolean stockUpdated = bookClient.updateAvailability(bookId, book.availableCopies() + 1);
            if (!stockUpdated) {
                log.warn("Failed to restore stock for bookId={} after return", bookId);
            }
        }
    }
}
