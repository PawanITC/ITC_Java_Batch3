package com.itc.funkart.payment.dto.response;

import com.itc.funkart.payment.entity.Payment;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for public-facing Payment details.
 * <p>
 * DESIGN NOTES:
 * 1. <b>Isolation:</b> This record separates our internal JPA Entity (Payment) from
 * the JSON response sent to the Frontend, preventing internal database
 * schema changes from breaking the API contract.
 * 2. <b>Factory Pattern:</b> Uses the static {@link #from(Payment)} method to
 * centralize mapping logic, ensuring consistency across all Controller endpoints.
 * 3. <b>Cents-Standard:</b> The 'amount' field represents the value in the
 * smallest currency unit (e.g., cents), matching Stripe's internal logic.
 * </p>
 */
public record PaymentResponse(
        Long id,
        Long userId,
        Long orderId,
        Long amount,      // Value in cents (e.g., 1999 for $19.99)
        String currency,  // ISO currency code (e.g., "usd")
        String status,    // Enum-like string: "pending", "succeeded", etc.
        LocalDateTime createdAt
) {
    /**
     * Maps a persistent Payment entity to a read-only Response record.
     * * @param payment The JPA Entity retrieved from the database.
     * @return A sanitized, immutable version of the payment data for the client.
     */
    public static PaymentResponse from(Payment payment) {
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