package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.event.PaymentRefundedEvent;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.response.ApiResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * <h2>PaymentService</h2>
 * <p>
 * The central business logic orchestrator for the Payment domain.
 * It manages the transition of payment states, ensures data ownership via JWT claims,
 * and synchronizes internal state with external Stripe events.
 * </p>
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_REFUNDED = "refunded";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final PaymentRepository paymentRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final StripeService stripeService;

    public PaymentService(PaymentRepository paymentRepository,
                          StripeService stripeService,
                          KafkaEventPublisher kafkaEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.stripeService = stripeService;
    }

    /**
     * Initializes a transaction by creating a local record and a Stripe intent.
     *
     * @param user    The authenticated user principal.
     * @param request Contains amount, currency, and order identification.
     * @return The Stripe client secret and intent metadata.
     */
    @Transactional
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(JwtUserDto user, CreatePaymentIntentRequest request) {
        try {
            // 1. Create local 'pending' record
            Payment payment = paymentRepository.save(
                    new Payment(user.id(), request.orderId(), request.amount(), request.currency()));

            // 2. Create Stripe Intent with local payment ID as metadata
            PaymentIntent stripeIntent = stripeService.createPaymentIntent(
                    request.amount(), request.currency(), user.id(), payment.getId());

            // 3. Link Stripe ID and persist
            payment.setStripePaymentIntentId(stripeIntent.getId());
            paymentRepository.save(payment);

            return new ApiResponse<>(PaymentIntentResponse.from(stripeIntent), "Payment intent created successfully");
        } catch (Exception ex) {
            throw new PaymentException("Failed to create payment intent: " + ex.getMessage());
        }
    }

    /**
     * Confirms the intent status after client-side payment method collection.
     *
     * @param user    The authenticated user principal.
     * @param request Contains the Stripe PaymentIntent and PaymentMethod ID.
     * @return The current state of the payment.
     */
    @Transactional
    public ApiResponse<PaymentResponse> confirmPayment(JwtUserDto user, ConfirmPaymentRequest request) {
        try {
            Payment payment = findPaymentForUser(request.paymentIntentId(), user);

            stripeService.confirmPaymentIntent(
                    request.paymentIntentId(), request.paymentMethodId(), request.returnUrl()
            );

            payment.setStatus(STATUS_PROCESSING);
            paymentRepository.save(payment);

            return new ApiResponse<>(PaymentResponse.from(payment), "Payment confirmation initiated");
        } catch (StripeException ex) {
            throw new PaymentException("Payment confirmation failed: " + ex.getMessage());
        }
    }

    /**
     * Securely retrieves a payment record for the logged-in user.
     *
     * @param user      The authenticated user principal.
     * @param paymentId The internal database ID.
     * @return The requested payment details.
     */
    public ApiResponse<PaymentResponse> getPayment(JwtUserDto user, Long paymentId) {
        Payment payment = findPaymentByIdAndUser(paymentId, user);
        return new ApiResponse<>(PaymentResponse.from(payment), "Payment retrieved successfully");
    }

    /**
     * Initiates a refund via Stripe and updates local records.
     *
     * @param user      The authenticated user principal.
     * @param paymentId Internal database ID of the transaction to refund.
     * @return The updated payment response.
     */
    @Transactional
    public ApiResponse<PaymentResponse> refundPayment(JwtUserDto user, Long paymentId) {
        try {
            Payment payment = findPaymentByIdAndUser(paymentId, user);

            if (!STATUS_SUCCEEDED.equalsIgnoreCase(payment.getStatus())) {
                throw new PaymentException("Can only refund payments that have succeeded.");
            }

            // Manual Refund (Admin/User Action)
            Refund stripeRefund = stripeService.refundPayment(payment.getStripePaymentIntentId());

            payment.setStatus(STATUS_REFUNDED);
            paymentRepository.save(payment);

            // Trigger Kafka event using the MANUAL factory (Refund object)
            kafkaEventPublisher.publishPaymentRefundedEvent(PaymentRefundedEvent.from(payment, stripeRefund));

            return new ApiResponse<>(PaymentResponse.from(payment), "Payment refund processed successfully");
        } catch (Exception e) {
            throw new PaymentException("Refund failed: " + e.getMessage());
        }
    }

    /**
     * Webhook Entry Point.
     * Transactional to ensure that DB updates and Kafka publishing happen atomically.
     */
    @Transactional
    public void processWebhookEvent(Event event) {
        logger.debug("Processing Stripe Event: {} | ID: {}", event.getType(), event.getId());

        // Use the RawJsonObject if the standard object deserializer fails
        // This is much more resilient for mixed API versions
        StripeObject stripeObject = getStripeObject(event);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSuccess((PaymentIntent) stripeObject);
            case "payment_intent.payment_failed" -> handlePaymentFailure((PaymentIntent) stripeObject);
            case "charge.refunded" -> handlePaymentRefunded((Charge) stripeObject);
            default -> logger.info("ℹ️ Received unhandled event type: {}", event.getType());
        }
    }

    @Nonnull
    private static StripeObject getStripeObject(Event event) {
        return event.getDataObjectDeserializer().getObject()
                .or(() -> Optional.ofNullable(event.getData().getObject())) // Try the deprecated fallback
                .orElseThrow(() -> new PaymentException("Could not deserialize Stripe event: " + event.getId()));
    }


    public void handlePaymentSuccess(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(intent.getId())
                .orElseThrow(() -> new PaymentException("Payment not found: " + intent.getId()));

        if (STATUS_SUCCEEDED.equals(payment.getStatus())) return; // Idempotency check

        payment.setStatus(STATUS_SUCCEEDED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentCompletedEvent(PaymentCompletedEvent.from(payment, intent));
        logger.info("✓ Payment SUCCESS | Order: {}", payment.getOrderId());
    }

    public void handlePaymentFailure(PaymentIntent intent) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(intent.getId())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        payment.setStatus(STATUS_FAILED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentFailedEvent(PaymentFailedEvent.from(payment, intent));
        logger.warn("✗ Payment FAILED | Intent: {}", intent.getId());
    }

    private void handlePaymentRefunded(Charge charge) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(charge.getPaymentIntent())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        payment.setStatus(STATUS_REFUNDED);
        paymentRepository.save(payment);

        // WEBHOOK Factory: Uses our charge.getRefunds() logic to get specific amount
        kafkaEventPublisher.publishPaymentRefundedEvent(PaymentRefundedEvent.from(payment, charge));
        logger.info("↺ Payment REFUNDED via Webhook | Intent: {}", charge.getPaymentIntent());
    }

    private Payment findPaymentForUser(String stripeId, JwtUserDto user) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripeId)
                .orElseThrow(() -> new PaymentException("Payment not found for Stripe ID: " + stripeId));

        if (!user.matches(payment.getUserId())) {
            throw new PaymentException("Unauthorized access to payment");
        }
        return payment;
    }

    private Payment findPaymentByIdAndUser(Long paymentId, JwtUserDto user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found for ID: " + paymentId));

        if (!user.matches(payment.getUserId())) {
            throw new PaymentException("Unauthorized access to payment");
        }
        return payment;
    }
}