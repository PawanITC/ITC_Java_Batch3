package com.itc.funkart.product_service.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderEvent {
    private String eventType; // "ORDER_CREATED"
    private Long userId;
    private BigDecimal totalAmount;
    private List<Long> productIds;
}
