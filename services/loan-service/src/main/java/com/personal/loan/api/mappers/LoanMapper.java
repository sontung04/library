package com.personal.loan.api.mappers;

import java.time.LocalDate;

import com.personal.loan.api.dtos.BookDto;
import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.api.dtos.UserDto;
import com.personal.loan.domain.entities.Book;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanStatus;
import com.personal.loan.domain.entities.User;

public class LoanMapper {

    private LoanMapper() {
    }

    public static LoanDto toDto(Loan loan) {
        return toDto(loan, null, null);
    }

    public static LoanDto toDto(Loan loan, Book book, User user) {
        if (loan == null)
            return null;

        LoanItemDto item;

        if (book == null)
            item = new LoanItemDto(
                    loan.getBookId(),
                    loan.getBookTitle(),
                    loan.getBookIsbn());
        else
            item = new LoanItemDto(
                    book.getId(),
                    book.getTitle(),
                    book.getIsbn());

        UserDto userDto;

        if (user == null)
            userDto = new UserDto(
                    loan.getUserId(),
                    loan.getUserUsername());
        else
            userDto = new UserDto(
                    user.getId(),
                    user.getUsername());

        return new LoanDto(
                loan.getId(),
                userDto,
                item,
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus());

    }

    public static Loan toEntity(Long userId, String username, CreateLoanRequest loanRequest,
            BookDto book) {
        if (loanRequest == null)
            return null;

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setUserUsername(username);
        loan.setBookId(loanRequest.bookId());
        loan.setBookTitle(book.title());
        loan.setBookIsbn(book.isbn());
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
                loanDto.user().id(),
                loanDto.user().username(),
                item != null ? item.bookId() : null,
                item != null ? item.bookTitle() : "unknown",
                item != null ? item.bookIsbn() : "unknown",
                loanDto.status(),
                loanDto.loanDate(),
                loanDto.dueDate(),
                loanDto.returnDate());
    }
}
