package com.itc.funkart.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itc.funkart.payment.entity.Payment;

import java.time.LocalDateTime;

/**
 * <h2>PaymentResponse</h2>
 * <p>
 * Data Transfer Object (DTO) for public-facing Payment details.
 * This record provides a sanitized view of the {@link Payment} entity for client consumption.
 * </p>
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li><b>Immutability:</b> Leverages Java Records for thread-safe, stack-efficient data handling.</li>
 *   <li><b>Precision:</b> Amounts are kept in cents (Long) to avoid floating-point errors in the JVM.</li>
 *   <li><b>Isolation:</b> Prevents internal JPA schema leaks to the Frontend.</li>
 * </ul>
 */
public record PaymentResponse(
        Long id,
        Long userId,
        Long orderId,
        Long amount,      // Value in cents (e.g., 1999 for $19.99)
        String currency,  // ISO currency code (e.g., "usd")
        String status,    // "PENDING", "SUCCEEDED", "FAILED", "REFUNDED"

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    /**
     * Static factory to map the internal JPA Entity to a safe Response DTO.
     *
     * @param payment The persistent Entity retrieved from the database.
     * @return A read-only representation of the payment status.
     */
    public static PaymentResponse from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}