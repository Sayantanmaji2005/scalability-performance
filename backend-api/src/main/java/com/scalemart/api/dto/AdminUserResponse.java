package com.scalemart.api.dto;

import java.time.Instant;

public record AdminUserResponse(
    Long id,
    String username,
    String email,
    String role,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
}
