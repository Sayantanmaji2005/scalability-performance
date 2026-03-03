package com.scalemart.api.dto;

import java.time.Instant;

public record ReviewResponse(
    Long id,
    String userId,
    Long productId,
    Integer rating,
    String comment,
    Instant createdAt,
    Instant updatedAt
) {}
