package com.itc.funkart.common.dto.auth.response.product;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Standard API response for product details.
 */
@Builder
public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String categoryName,
        List<String> imageUrls,
        Boolean active,
        String brand
) implements Serializable {
}
