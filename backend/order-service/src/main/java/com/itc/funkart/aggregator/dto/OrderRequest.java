package com.itc.funkart.aggregator.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * <h2>OrderRequest</h2>
 * <p>The primary payload for creating a new order. Contains a list of
 * items selected by the user from their shopping cart.</p>
 */
@Data
public class OrderRequest {

    /**
     * A collection of one or more product items to be included in the order.
     */
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items;
}