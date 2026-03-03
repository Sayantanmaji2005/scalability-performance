package com.scalemart.api.service;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderEvent(
    Long orderId,
    String userId,
    Long productId,
    Integer quantity,
    BigDecimal totalAmount,
    Instant createdAt
) {
}
