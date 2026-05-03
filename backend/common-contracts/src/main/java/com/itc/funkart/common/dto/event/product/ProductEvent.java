package com.itc.funkart.common.dto.event.product;

import com.itc.funkart.common.dto.auth.response.product.ProductResponse;
import com.itc.funkart.common.enums.product.ProductEventType;
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
