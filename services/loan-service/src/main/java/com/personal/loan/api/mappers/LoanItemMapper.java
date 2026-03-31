package com.personal.loan.api.mappers;

import com.personal.loan.api.dtos.LoanItemDto;
import com.personal.loan.domain.entities.LoanItem;

public class LoanItemMapper {

    private LoanItemMapper() {}

    public static LoanItemDto toDto(LoanItem item) {
        if (item == null)
            return null;

        return new LoanItemDto(item.getBookId(), item.getAmount(), null);
    }

    public static LoanItem toEntity(LoanItemDto loanItemDto) {
        if (loanItemDto == null)
            return null;

        LoanItem loanItem = new LoanItem();
        loanItem.setBookId(loanItemDto.bookId());
        loanItem.setAmount(loanItemDto.amount());
        return loanItem;
    } 
}
