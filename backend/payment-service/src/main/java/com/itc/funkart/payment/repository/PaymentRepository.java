package com.itc.funkart.payment.repository;

import com.itc.funkart.payment.entity.Payment;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>PaymentRepository</h2>
 * <p>
 * Spring Data JPA repository for {@link Payment} entities.
 * Provides abstraction for SQL operations, allowing the JVM to manage
 * connection pooling and transaction boundaries efficiently.
 * </p>
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Locates a payment by its external Stripe reference.
     * <p>
     * <b>Optimization:</b> This query relies on the unique index {@code idx_stripe_pi}
     * defined in the entity for O(1) or O(log n) lookup speed.
     * </p>
     *
     * @param stripePaymentIntentId The Stripe ID (pi_...).
     * @return An {@link Optional} containing the payment if found.
     */
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Locates a payment associated with a specific order.
     *
     * @param orderId The Order ID to search for.
     * @return An {@link Optional} containing the payment.
     */
    Optional<Payment> findByOrderId(@NotNull(message = "Order ID is required") Long orderId);

    /**
     * Checks if a payment already exists for a given order.
     * Used for idempotency checks before initiating new Stripe Intents.
     *
     * @param orderId The Order ID.
     * @return boolean true if a record exists.
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Example of an optimized partial fetch.
     * Retrieves only the status of a payment to minimize JVM memory overhead
     * during high-volume webhook processing.
     *
     * @param stripeId The Stripe ID.
     * @return The status string.
     */
    @Query("SELECT p.status FROM Payment p WHERE p.stripePaymentIntentId = :stripeId")
    Optional<String> findStatusByStripeId(@Param("stripeId") String stripeId);
}