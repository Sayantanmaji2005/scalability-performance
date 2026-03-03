package com.scalemart.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
    Long orderId,
    String status,
    BigDecimal totalAmount,
    Instant createdAt
) {
}
