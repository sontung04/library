package com.personal.book.events;

public record BookLifecycleEvent(
        Long bookId,
        String title,
        String isbn,
        boolean deleted) {
}
