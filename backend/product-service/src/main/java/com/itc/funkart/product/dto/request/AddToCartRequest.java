package com.itc.funkart.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Data transfer object for adding an item to the shopping cart.
 */
@Builder
public record AddToCartRequest(
        @NotNull(message = "Product is required") Long productId,
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {
}