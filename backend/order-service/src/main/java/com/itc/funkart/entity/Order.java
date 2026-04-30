
package com.itc.funkart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_status", columnList = "order_status"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_correlation_id", columnList = "correlation_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private UUID customerId;

    private UUID productId;

    private Integer quantity;

    private Double price;

    private String orderStatus;

    // === NEW: Correlation ID for tracing ===
    private String correlationId;

    // === NEW: Track processed event IDs for idempotency ===
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_processed_events", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "event_id")
    private Set<String> processedEventIds = new HashSet<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // === NEW: Helper methods for idempotency ===
    public boolean hasProcessedEvent(String eventId) {
        return processedEventIds != null && processedEventIds.contains(eventId);
    }

    public void recordProcessedEvent(String eventId) {
        if (processedEventIds == null) {
            processedEventIds = new HashSet<>();
        }
        processedEventIds.add(eventId);
    }
}
