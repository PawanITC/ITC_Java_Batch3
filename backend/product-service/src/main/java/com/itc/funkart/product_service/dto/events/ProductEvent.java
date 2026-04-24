package com.itc.funkart.product_service.dto.events;

import com.itc.funkart.product_service.dto.response.ProductResponse;
import com.itc.funkart.product_service.enums.ProductEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {
    private ProductEventType eventType; // e.g., "PRODUCT_CREATED"
    private ProductResponse product; // The product data associated with the event
    private long id;
}
