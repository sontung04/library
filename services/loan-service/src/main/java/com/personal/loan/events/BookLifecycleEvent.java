package com.personal.loan.events;

public record BookLifecycleEvent(
        Long bookId,
        String title,
        String isbn,
        boolean deleted) {
}
