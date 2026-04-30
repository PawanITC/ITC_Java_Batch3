package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * <h2>OrderItemResponse</h2>
 * <p>
 * Represents the public-facing view of a specific product within an order.
 * This DTO provides the "Line Item" details for the frontend.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    /**
     * The unique identifier of the line item record.
     */
    private Long id;

    /**
     * The ID of the product purchased.
     * (Frontend can use this to link back to the Product Detail Page).
     */
    private Long productId;

    /**
     * The quantity of this specific product purchased.
     */
    private Integer quantity;

    /**
     * The price of the product at the exact moment of purchase.
     * <p>Note: This may differ from the current product price in the catalog.</p>
     */
    private BigDecimal priceAtPurchase;

    /**
     * Optional helper: The calculated subtotal for this line item (price * quantity).
     * Useful for frontend display without requiring extra JS logic.
     */
    public BigDecimal getSubTotal() {
        if (priceAtPurchase == null || quantity == null) return BigDecimal.ZERO;
        return priceAtPurchase.multiply(BigDecimal.valueOf(quantity));
    }
}
