package com.personal.user.events;

public record UserLifecycleEvent(
        Long userId,
        String username,
        boolean deleted) {
}
