package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.event.PaymentRefundedEvent;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.response.ApiResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Core Orchestrator for the FunKart Payment Ecosystem.
 * <p>
 * DESIGN PRINCIPLES:
 * 1. Synchronous API: Intent creation, confirmation, and manual ↺ refunds.
 * 2. Asynchronous Webhooks: Listens to Stripe events to update DB and notify Kafka.
 * 3. Security: Ensures users can only access or refund their own payments.
 * </p>
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    // ================= STATUS CONSTANTS =================
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

    // ================= CREATE PAYMENT INTENT =================
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(Long userId, CreatePaymentIntentRequest request) {
        try {
            // Save local record first to get our internal ID
            Payment payment = paymentRepository.save(
                    new Payment(userId, request.orderId(), request.amount(), request.currency()));

            // Call Stripe with our local payment ID as the Idempotency Key
            PaymentIntent stripeIntent = stripeService.createPaymentIntent(
                    request.amount(), request.currency(), userId, payment.getId());

            payment.setStripePaymentIntentId(stripeIntent.getId());
            paymentRepository.save(payment);

            return new ApiResponse<>(PaymentIntentResponse.from(stripeIntent), "Payment intent created successfully");

        } catch (StripeException ex) {
            throw new PaymentException("Stripe error: " + ex.getMessage());
        }
    }

    // ================= CONFIRM PAYMENT =================
    public ApiResponse<PaymentResponse> confirmPayment(Long userId, ConfirmPaymentRequest request) {
        try {
            Payment payment = findPaymentForUser(request.paymentIntentId(), userId);

            PaymentIntent confirmedIntent = stripeService.confirmPaymentIntent(
                    request.paymentIntentId(), request.paymentMethodId(), request.returnUrl()
            );

            payment.setStatus(STATUS_PROCESSING);
            paymentRepository.save(payment);

            return new ApiResponse<>(PaymentResponse.from(payment), "Payment confirmation initiated");

        } catch (StripeException ex) {
            throw new PaymentException("Payment confirmation failed: " + ex.getMessage());
        }
    }

    // ================= REFUND PAYMENT (Manual Trigger) =================
    public ApiResponse<PaymentResponse> refundPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentByUser(paymentId, userId);

        if (!STATUS_SUCCEEDED.equals(payment.getStatus())) {
            throw new PaymentException("Can only refund succeeded payments");
        }

        try {
            // CALL: Actually trigger the refund in Stripe (returns a Refund object)
            Refund stripeRefund = stripeService.refundPayment(payment.getStripePaymentIntentId());

            payment.setStatus(STATUS_REFUNDED);
            paymentRepository.save(payment);

            // EVENT: Uses the Refund-specific factory overload ↺
            kafkaEventPublisher.publishPaymentRefundedEvent(
                    PaymentRefundedEvent.from(payment, stripeRefund));

            return new ApiResponse<>(PaymentResponse.from(payment), "Payment refunded successfully");
        } catch (StripeException ex) {
            throw new PaymentException("Stripe rejected the refund: " + ex.getMessage());
        }
    }

    // ================= GET PAYMENT =================
    public ApiResponse<PaymentResponse> getPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentByUser(paymentId, userId);
        return new ApiResponse<>(PaymentResponse.from(payment), "Payment retrieved successfully");
    }

    // ================= HANDLE WEBHOOK EVENTS (Success) =================
    @Transactional
    public void handlePaymentSuccess(PaymentIntent intent) {
        String stripeId = intent.getId();
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripeId)
                .orElseThrow(() -> new PaymentException("CRITICAL: Received success webhook for unknown Stripe ID: " + stripeId));

        if (STATUS_SUCCEEDED.equals(payment.getStatus())) {
            logger.info("Ignored duplicate success webhook for Stripe ID: {}", stripeId);
            return;
        }

        payment.setStatus(STATUS_SUCCEEDED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentCompletedEvent(PaymentCompletedEvent.from(payment, intent));

        logger.info("✓ Payment SUCCEEDED | Local ID: {} | Stripe ID: {} | User: {} | Order: {}",
                payment.getId(), stripeId, payment.getUserId(), payment.getOrderId());
    }

    // ================= HANDLE WEBHOOK EVENTS (Failure) =================
    public void handlePaymentFailure(PaymentIntent intent) {
        String stripeId = intent.getId();
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripeId)
                .orElseThrow(() -> new PaymentException("Payment not found for Stripe ID " + stripeId));

        if (STATUS_FAILED.equals(payment.getStatus())) {
            logger.info("Payment already failed, skipping Stripe ID: {}", stripeId);
            return;
        }

        payment.setStatus(STATUS_FAILED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentFailedEvent(
                PaymentFailedEvent.from(payment, intent)
        );

        logger.warn("✗ Payment failure handled for Stripe ID: {}", stripeId);
    }

    // ================= HANDLE WEBHOOK EVENTS (Refunded) =================
    public void handlePaymentRefunded(Charge charge) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(charge.getPaymentIntent())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        if (STATUS_REFUNDED.equals(payment.getStatus())) {
            logger.info("Payment already marked as refunded, skipping: {}", charge.getPaymentIntent());
            return;
        }

        payment.setStatus(STATUS_REFUNDED);
        paymentRepository.save(payment);

        // EVENT: Uses the Charge-specific factory overload ↺
        kafkaEventPublisher.publishPaymentRefundedEvent(
                PaymentRefundedEvent.from(payment, charge)
        );

        logger.info("↺ Payment refund handled for Stripe ID: {}", charge.getPaymentIntent());
    }

    // ================= HELPER METHODS =================
    private Payment findPaymentForUser(String stripePaymentIntentId, Long userId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized access to payment");
        }
        return payment;
    }

    private Payment findPaymentByUser(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found"));
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentException("Unauthorized access to payment");
        }
        return payment;
    }
}