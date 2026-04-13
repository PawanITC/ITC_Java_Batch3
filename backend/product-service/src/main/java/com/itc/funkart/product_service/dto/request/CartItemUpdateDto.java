package com.itc.funkart.product_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemUpdateDto {
    @NotNull(message = "Quantity adjustment is required")
    // This allows -1 or 1
    private Integer quantityChange;
}
