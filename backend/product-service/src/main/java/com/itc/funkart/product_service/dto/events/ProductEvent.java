package com.itc.funkart.product_service.dto.events;

import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.enums.ProductEventType;
import lombok.Builder;

/**
 * Event published by the Product Service to notify other services
 * of changes in the product catalog.
 */
@Builder
public record ProductEvent(
        ProductEventType eventType,
        ProductResponse product,
        long id
) {
}