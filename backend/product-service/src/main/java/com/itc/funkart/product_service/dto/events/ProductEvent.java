package com.itc.funkart.product_service.dto.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEvent {
    private String eventType; // e.g., "PRODUCT_CREATED"
    private Long id;
    private String name;
}
