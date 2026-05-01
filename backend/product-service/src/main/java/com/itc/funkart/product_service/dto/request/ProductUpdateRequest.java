package com.itc.funkart.product_service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Data transfer object for updating existing product details.
 * All fields except validation constraints are optional to allow partial updates (PATCH style).
 */
@Builder
public record ProductUpdateRequest(
        @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
        String name,
        String description,
        @Positive(message = "Price must be positive")
        BigDecimal price,
        @Min(value = 0, message = "Stock cannot be negative")
        Integer stockQuantity,
        @Size(min = 2, message = "Brand name is too short")
        String brand,
        Boolean active,
        Long categoryId
) {
}