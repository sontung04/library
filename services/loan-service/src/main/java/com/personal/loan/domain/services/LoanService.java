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
    private final UserClient userClient;

    @Transactional
    public LoanDto createLoan(Long userId, CreateLoanRequest request) {
        log.info("Executing LoanService::createLoan");

        LoanItemDto item = new LoanItemDto(request.bookId(), "", "", false);

        if (!checkBooksStatus(items))
            throw new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK);

        BookDto book = bookClient.getBook(request.bookId());
        if (book == null) {
            throw new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK);
        }
        String username = userClient.getUsername(userId);

        items.forEach(this::subtractBookAmountFromDb);

        Loan createdLoan = loanRepository.save(LoanMapper.toEntity(userId, username, request, book));

        return enrichLoan(createdLoan);
    }

    private boolean checkBooksStatus(Long bookId) {
        BookDto book = bookClient.getBook(bookId);
        
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

    public boolean hasActiveLoans(Long userId) {
        return loanRepository.existsByUserIdAndStatus(userId, LoanStatus.ACTIVE);
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

        restoreBookStock(loan.getBookId());
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        return enrichLoan(loanRepository.save(loan));
    }

    private LoanDto enrichLoan(Loan loan) {
        LoanDto baseLoan = LoanMapper.toDto(loan);
        List<LoanItemDto> enrichedItems = baseLoan.items().stream()
                .map(item -> new LoanItemDto(
                        item.bookId(),
                        item.bookTitle(),
                        item.bookIsbn(),
                        item.bookDeleted(),
                        item.amount(),
                        item.bookDeleted() ? null : bookClient.getBook(item.bookId())))
                .toList();

        return new LoanDto(
                baseLoan.id(),
                baseLoan.userId(),
                baseLoan.userUsername(),
                baseLoan.userDeleted(),
                enrichedItems,
                baseLoan.loanDate(),
                baseLoan.dueDate(),
                baseLoan.returnDate(),
                baseLoan.status());
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
            bookClient.updateAvailability(bookId, book.availableCopies() + 1);
        }
    }
}
