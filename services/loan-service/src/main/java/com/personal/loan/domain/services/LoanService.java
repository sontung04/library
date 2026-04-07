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
     * Applies a user lifecycle event (update or deletion) to all of the user's loan
     * snapshot records.
     * Triggered by a Kafka {@code UserUpdatedEvent} or {@code UserDeletedEvent}
     * consumed from user-service.
     *
     * <p>
     * On update, the stored {@code userUsername} is refreshed to the new value.
     * On deletion, {@code userDeleted} is flagged so that the loan history remains
     * queryable
     * without a foreign key back to user-service.
     *
     * @param userId   the ID of the affected user
     * @param username the user's current (possibly new) username
     * @param deleted  {@code true} if the user was deleted; {@code false} if only
     *                 updated
     */
    @Transactional
    public void applyUserLifecycleEvent(Long userId, String username, boolean deleted) {
        List<Loan> loans = loanRepository.findByUserId(userId);
        loans.forEach(loan -> {
            loan.setUserUsername(username);
            loan.setUserDeleted(deleted);
        });
        loanRepository.saveAll(loans);
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
