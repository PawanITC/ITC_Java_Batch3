package com.itc.funkart.product_service.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Response representing an individual item within a cart.
 */
@Builder
public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        BigDecimal subTotal
) {
}