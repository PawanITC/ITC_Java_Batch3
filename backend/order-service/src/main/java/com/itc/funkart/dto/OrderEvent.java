
package com.itc.funkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    // === Event Metadata ===
    private String eventId;           // Unique event ID for idempotency
    private String eventType;         // order.created, order.updated, order.cancelled
    private int eventVersion;         // Schema versioning
    private LocalDateTime eventTime;
    private String correlationId;     // Trace across services
    private String source;            // "order-service"

    // === Order Data ===
    private UUID orderId;
    private UUID customerId;
    private UUID productId;
    private Integer quantity;
    private Double price;
    private String orderStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

