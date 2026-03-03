package com.scalemart.api.dto;

import java.time.Instant;

public record WishlistResponse(
    Long id,
    Long productId,
    String productName,
    String productCategory,
    java.math.BigDecimal productPrice,
    Instant addedAt
) {}
