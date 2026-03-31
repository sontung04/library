package com.personal.loan.api.mappers;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.personal.loan.api.dtos.CreateLoanRequest;
import com.personal.loan.api.dtos.LoanDto;
import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.domain.entities.Loan;
import com.personal.loan.domain.entities.LoanItem;
import com.personal.loan.domain.entities.LoanStatus;

public class LoanMapper {

    private LoanMapper() {}
    
    public static LoanDto toDto(Loan loan) {
        if (loan == null)
            return null;

        List<LoanItemDto> items = loan.getItems() == null
                ? Collections.emptyList()
                : loan.getItems()
                .stream()
                .map(LoanItemMapper::toDto)
                .toList(); 

        return new LoanDto(
            loan.getId(),
            loan.getUserId(),
            items,
            loan.getLoanDate(),
            loan.getDueDate(),
            loan.getReturnDate(),
            loan.getStatus()
        );
    }

    public static Loan toEntity(Long userId, CreateLoanRequest loanRequest) {
        if (loanRequest == null) 
            return null;

        LoanItem loanItem = new LoanItem();
        loanItem.setBookId(loanRequest.bookId());
        loanItem.setAmount(1);

        List<LoanItem> items = Collections.singletonList(loanItem);

        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setItems(items);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(loanRequest.dueDate() != null ? loanRequest.dueDate() : LocalDate.now().plusDays(14));
        loan.setStatus(LoanStatus.ACTIVE);

        return loan;
    }

    public static Loan toEntity(LoanDto loanDto) {
        if (loanDto == null) 
            return null;

        List<LoanItem> items = loanDto.items() == null
                ? Collections.emptyList()
                : loanDto.items()
                .stream()
                .map(LoanItemMapper::toEntity)
                .toList();
                
        return new Loan(
            loanDto.id(),
            loanDto.userId(),
            items,
            loanDto.status(),
            loanDto.loanDate(),
            loanDto.dueDate(),
            loanDto.returnDate()
        );
    }
}
