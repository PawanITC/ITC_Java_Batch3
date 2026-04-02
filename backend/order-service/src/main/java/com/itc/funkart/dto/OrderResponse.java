package com.itc.funkart.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {

    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private Integer quantity;
    private Double price;
    private String orderStatus;
    private String eventStatus;
//    private String message;
}