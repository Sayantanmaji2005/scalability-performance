package com.scalemart.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
    Long id,
    Long userId,
    List<CartItemResponse> items,
    BigDecimal total,
    int itemCount
) {
    public record CartItemResponse(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
