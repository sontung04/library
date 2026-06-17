package com.personal.user.dtos;

import java.util.List;

public record InternalUserAuthSnapshotResponse(
        Long userId,
        String username,
        List<String> roles) {
}
