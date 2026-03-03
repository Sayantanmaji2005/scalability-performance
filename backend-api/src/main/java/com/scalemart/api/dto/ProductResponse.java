package com.scalemart.api.dto;

import java.math.BigDecimal;
import java.io.Serializable;

public record ProductResponse(
    Long id,
    String name,
    String description,
    String category,
    BigDecimal price,
    Integer stock,
    String imageUrl,
    Double averageRating,
    Integer reviewCount,
    Boolean isActive
) implements Serializable {
}
