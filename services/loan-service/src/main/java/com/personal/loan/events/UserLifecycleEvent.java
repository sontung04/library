package com.personal.loan.events;

public record UserLifecycleEvent(
        Long userId,
        String username,
        boolean deleted) {
}
