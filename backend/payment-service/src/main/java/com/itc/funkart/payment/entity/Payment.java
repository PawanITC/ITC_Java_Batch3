package com.itc.funkart.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <h2>Payment Entity</h2>
 * <p>
 * Represents the persistent state of a payment transaction within the FunKart ecosystem.
 * This entity maps to the {@code payments} table and tracks the lifecycle of a
 * Stripe {@code PaymentIntent}.
 * </p>
 *
 * <p><b>Lifecycle Management:</b></p>
 * <ul>
 *   <li><b>Creation:</b> Handled via the custom constructor or JPA {@link #onCreate()}.</li>
 *   <li><b>Updates:</b> {@code updatedAt} is automatically refreshed via {@link #onUpdate()}.</li>
 * </ul>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payments", indexes = {
        @Index(name = "idx_stripe_pi", columnList = "stripePaymentIntentId"),
        @Index(name = "idx_order_id", columnList = "orderId")
})
public class Payment {

    /**
     * Internal primary key. Uses {@link GenerationType#IDENTITY} to delegate
     * ID generation to the database (e.g., MySQL Auto-Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the user who initiated the transaction.
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * The unique reference to the Order being paid for.
     */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /**
     * Total amount in the smallest currency unit (e.g., cents).
     */
    @Column(nullable = false)
    private Long amount;

    /**
     * ISO 4217 currency code (e.g., "usd").
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * The external reference ID provided by Stripe (pi_...). indexed for fast lookups.
     */
    @Column(unique = true)
    private String stripePaymentIntentId;

    /**
     * Stable UUID used as the Stripe idempotency key when creating a PaymentIntent.
     * Generated once on entity creation; survives Kafka retries within the same DB lifecycle.
     * A DB wipe produces a new UUID → new Stripe key → no IdempotencyException on replay.
     */
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String idempotencyKey;

    /**
     * Current transaction status (e.g., PENDING, SUCCEEDED, FAILED, REFUNDED).
     */
    @Column(nullable = false)
    private String status;

    /**
     * Timestamp when the record was first persisted in the JVM.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last state change.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convenience constructor for initializing a new transaction.
     *
     * @param userId   User ID.
     * @param orderId  Order ID.
     * @param amount   Amount in cents.
     * @param currency Currency code.
     */
    public Payment(Long userId, Long orderId, Long amount, String currency) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = "PENDING";
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    /**
     * Automatically sets timestamps before the entity is saved to the database.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Refreshes the update timestamp before any SQL UPDATE statement is executed.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}