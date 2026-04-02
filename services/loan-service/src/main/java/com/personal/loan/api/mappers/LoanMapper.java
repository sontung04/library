package com.personal.loan.api.mappers;

import java.time.LocalDate;

import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;

public class LoanMapper {

    private LoanMapper() {
    }

    public static LoanDto toDto(Loan loan) {
        if (loan == null)
            return null;

        LoanItemDto item = new LoanItemDto(
                loan.getBookId(),
                loan.getBookTitle(),
                loan.getBookIsbn(),
                loan.isBookDeleted());

        return new LoanDto(
                loan.getId(),
                loan.getUserId(),
                loan.getUserUsername(),
                loan.isUserDeleted(),
                item,
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus());
    }

    public static Loan toEntity(Long userId, String username, CreateLoanRequest loanRequest,
            com.personal.loan.api.dtos.BookDto book) {
        if (loanRequest == null)
            return null;

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setUserUsername(username);
        loan.setUserDeleted(false);
        loan.setBookId(loanRequest.bookId());
        loan.setBookTitle(book.title());
        loan.setBookIsbn(book.isbn());
        loan.setBookDeleted(false);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(loanRequest.dueDate() != null ? loanRequest.dueDate() : LocalDate.now().plusDays(14));
        loan.setStatus(LoanStatus.ACTIVE);

        return loan;
    }

    public static Loan toEntity(LoanDto loanDto) {
        if (loanDto == null)
            return null;

        LoanItemDto item = loanDto.item();

        return new Loan(
                loanDto.id(),
                loanDto.userId(),
                loanDto.userUsername(),
                loanDto.userDeleted(),
                item != null ? item.bookId() : null,
                item != null ? item.bookTitle() : "unknown",
                item != null ? item.bookIsbn() : "unknown",
                item != null && item.bookDeleted(),
                loanDto.status(),
                loanDto.loanDate(),
                loanDto.dueDate(),
                loanDto.returnDate());
    }
}
