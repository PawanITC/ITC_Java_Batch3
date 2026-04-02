

package com.itc.funkart.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OrderEvent {
    private String eventType;
    private UUID orderId;
    private UUID customerId;
    private LocalDateTime timestamp;
}