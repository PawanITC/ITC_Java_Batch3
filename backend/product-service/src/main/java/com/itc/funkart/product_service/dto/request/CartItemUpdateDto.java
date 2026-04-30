package com.itc.funkart.product_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * DTO for incrementing or decrementing cart item quantity.
 */
@Builder
public record CartItemUpdateDto(
        @NotNull(message = "Quantity adjustment is required") Integer quantityChange
) {
}