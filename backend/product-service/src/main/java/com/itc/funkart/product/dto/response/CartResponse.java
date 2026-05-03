package com.itc.funkart.product.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Comprehensive response for the user's shopping cart.
 * Using Lombok @Builder to facilitate easy object creation in tests.
 */
@Builder
public record CartResponse(
        Long cartId,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
}