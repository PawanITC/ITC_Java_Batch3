package com.itc.funkart.product_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request payload for creating a new product.
 */
@Builder
@Jacksonized // Senior Tip: Helps Jackson (JSON parser) use the Builder for deserialization
public record ProductCreateRequest(
        @NotBlank(message = "Product name is required") @Size(min = 3, max = 100) String name,
        @NotBlank(message = "Description is required") String description,
        @NotNull(message = "Price is required") @Positive BigDecimal price,
        @NotNull(message = "Stock quantity is required") @Min(0) Integer stockQuantity,
        @NotNull(message = "Category ID is required") Long categoryId,
        List<String> imageUrls,
        @NotNull(message = "Brand is required") String brand
) {
}