package com.personal.book.events;

public record BookAvailabilityEvent(
        String eventId,
        Long bookId,
        LoanAction action) {
}
