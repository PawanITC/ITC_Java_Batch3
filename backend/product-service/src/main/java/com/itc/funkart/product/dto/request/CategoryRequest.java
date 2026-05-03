package com.itc.funkart.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request for creating or updating a product category.
 */
@Builder
public record CategoryRequest(
        @NotBlank(message = "Category name is required") String name,
        @NotBlank(message = "Description is required") String description
) {
}