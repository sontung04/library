package com.personal.loan.domain.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.api.mappers.BookMapper;
import com.personal.loan.api.mappers.LoanMapper;
import com.personal.loan.client.BookClient;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.entities.User;
import com.personal.loan.domain.exception.ErrorCode;
import com.personal.loan.domain.exception.WebException;
import com.personal.loan.domain.repositories.BookRepository;
import com.personal.loan.domain.repositories.LoanRepository;
import com.personal.loan.domain.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookClient bookClient;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Creates a new active loan for a user.
     *
     * <p>
     * Steps performed:
     * <ol>
     * <li>Fetches the book from book-service and verifies at least one copy is
     * available.</li>
     * <li>Decrements the book's available copy count via book-service.</li>
     * <li>Resolves the user's display name from user-service for the loan
     * snapshot.</li>
     * <li>Persists the loan record with status {@link LoanStatus#ACTIVE}.</li>
     * </ol>
     *
     * @param userId  the ID of the borrowing user, injected from the
     *                {@code X-User-Id} gateway header
     * @param request the creation payload containing the {@code bookId} to borrow
     * @return a {@link LoanDto} representing the newly created loan
     * @throws WebException with {@link ErrorCode#INSUFFICIENT_BOOK_STOCK} if no
     *                      copies are available
     * @throws WebException with {@link ErrorCode#UNCATEGORIZED_EXCEPTION} if the
     *                      stock update call fails
     */
    @Transactional
    public LoanDto createLoan(Long userId, CreateLoanRequest request) {
        log.info("Executing LoanService::createLoan");

        BookDto bookDto;
        try {
            bookDto = bookRepository.findById(request.bookId())
                    .map(BookMapper::toDto)
                    .get();
        } catch (NoSuchElementException e) {
            log.info("Cannot find book {} from loan service's database.", request.bookId());
            bookDto = bookClient.getBook(request.bookId());
        }

        if (!checkBooksAvailability(bookDto))
            throw new WebException(ErrorCode.INSUFFICIENT_BOOK_STOCK);

        LoanItemDto item = new LoanItemDto(
                request.bookId(),
                bookDto.title(),
                bookDto.isbn());

        subtractBookAmountFromDb(item, bookDto);

        // Return null to username if user cannot be found
        String username = userRepository.findById(userId)
                .map(User::getUsername)
                .orElseGet(() -> {
                    log.info("Cannot find user with id = {}", userId);
                    return null;
                });

        Loan createdLoan = loanRepository.save(LoanMapper.toEntity(userId, username, request, bookDto));
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

    /**
     * Returns all loans belonging to a specific user, in repository order.
     *
     * @param userId the ID of the user whose loan history to fetch
     * @return a list of {@link LoanDto}; empty if the user has no loans
     */
    @Transactional(readOnly = true)
    public List<LoanDto> getUserLoans(Long userId) {
        List<Loan> userLoans = loanRepository.findByUserId(userId);

        if (userLoans.isEmpty())
            return Collections.emptyList();

        return userLoans.stream()
                .map(LoanMapper::toDto)
                .toList();
    }

    /**
     * Checks whether a user currently has any unreturned loans.
     * Called by user-service via the internal API before allowing account deletion.
     *
     * @param userId the ID of the user to check
     * @return {@code true} if at least one {@link LoanStatus#ACTIVE} loan exists
     *         for the user
     */
    @Transactional(readOnly = true)
    public boolean hasActiveLoans(Long userId) {
        return loanRepository.existsByUserIdAndStatus(userId, LoanStatus.ACTIVE);
    }

    /**
     * Returns every loan in the system regardless of user or status.
     * Intended for admin/librarian overview endpoints.
     *
     * @return a list of all {@link LoanDto}; empty if no loans exist
     */
    @Transactional(readOnly = true)
    public List<LoanDto> getAllLoans() {
        List<Loan> loans = loanRepository.findAll();

        if (loans.isEmpty())
            return Collections.emptyList();

        return loans.stream()
                .map(LoanMapper::toDto)
                .toList();
    }

    /**
     * Marks an active loan as returned and restores book stock.
     *
     * <p>
     * Steps performed:
     * <ol>
     * <li>Verifies the loan exists and has status {@link LoanStatus#ACTIVE}.</li>
     * <li>Increments the book's available copy count via book-service.</li>
     * <li>Sets the loan status to {@link LoanStatus#RETURNED} and records today as
     * the return date.</li>
     * </ol>
     *
     * @param loanId the ID of the loan to return
     * @return the updated {@link LoanDto} with status {@code RETURNED} and the
     *         return date set
     * @throws WebException with {@link ErrorCode#LOAN_NOT_FOUND} if the loan does
     *                      not exist
     * @throws WebException with {@link ErrorCode#LOAN_NOT_ACTIVE} if the loan is
     *                      already returned
     */
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

    /**
     * Applies a book deletion event to all loan records that reference the deleted
     * book.
     * Triggered by a Kafka {@code BookDeletedEvent} consumed from book-service.
     *
     * <p>
     * The stored {@code bookTitle} and {@code bookIsbn} are updated to the last
     * known values
     * and {@code bookDeleted} is flagged, preserving the loan history without a
     * live book record.
     *
     * @param bookId the ID of the deleted book
     * @param title  the book's title at the time of deletion
     * @param isbn   the book's ISBN at the time of deletion
     */
    @Transactional
    public void deleteBookId(Long bookId) {
        List<Loan> loans = loanRepository.findByBookId(bookId);
        loans.forEach(loan -> loan.setBookId(null));
        loanRepository.saveAll(loans);
    }

    private void restoreBookStock(Long bookId) {

        if (bookId == null) {
            log.error("Trying to return an already deleted book");
            return;
        }

        BookDto book = bookClient.getBook(bookId);
        if (book != null) {
            boolean stockUpdated = bookClient.updateAvailability(bookId, book.availableCopies() + 1);
            if (!stockUpdated) {
                log.warn("Failed to restore stock for bookId={} after return", bookId);
            }
        }
    }

    public void deleteUserId(Long userId) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        loans.forEach(loan -> loan.setUserId(null));
        loanRepository.saveAll(loans);
    }
}
