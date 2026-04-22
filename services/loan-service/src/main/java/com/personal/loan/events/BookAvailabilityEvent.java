package com.personal.loan.events;

import java.util.UUID;

public record BookAvailabilityEvent(
        String eventId,
        Long bookId,
        LoanAction action) {
    public BookAvailabilityEvent(Long bookId, LoanAction action) {
        this(UUID.randomUUID().toString(), bookId, action);
    }
}
