package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private Integer quantity;
    private Double price;
    private String orderStatus;
}