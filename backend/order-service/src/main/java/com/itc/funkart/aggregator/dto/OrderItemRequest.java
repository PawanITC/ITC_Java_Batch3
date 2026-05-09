package com.itc.funkart.aggregator.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h2>OrderItemRequest</h2>
 * <p>Represents a single product line-item sent from the client (frontend)
 * during the checkout process.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    /**
     * The unique identifier for the product being purchased.
     */
    @NotNull(message = "Product ID is required")
    private Long productId;

    /**
     * The number of units requested.
     * Must be at least 1.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}