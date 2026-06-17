package com.personal.book.domain.services;

public interface BookAvailabilityEventHandler {
    void handleLoanEvent(String eventId, Long bookId);

    void handleReturnEvent(String eventId, Long bookId);
}
