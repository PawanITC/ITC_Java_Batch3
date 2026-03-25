package com.itc.funkart.payment.service;

import com.itc.funkart.payment.dto.event.PaymentCompletedEvent;
import com.itc.funkart.payment.dto.event.PaymentFailedEvent;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.response.ApiResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    // ================= STATUS CONSTANTS =================
    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_REFUNDED = "refunded";

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
            // ✅ Generate a unique idempotency key per request
            String idempotencyKey = "payment-intent-" + userId + "-" + request.orderId() + "-" + UUID.randomUUID();

            // ✅ Create Stripe PaymentIntent first
            PaymentIntent stripeIntent = stripeService.createPaymentIntent(
                    request.amount(),
                    request.currency(),
                    userId,
                    request.orderId(),   // you can use orderId or temp paymentId
                    idempotencyKey       // pass the unique idempotency key
            );

            // ✅ Now persist Payment in DB, including Stripe ID
            Payment payment = new Payment(userId, request.orderId(), request.amount(), request.currency());
            payment.setStripePaymentIntentId(stripeIntent.getId());
            paymentRepository.save(payment);

            return new ApiResponse<>(
                    new PaymentIntentResponse(
                            stripeIntent.getClientSecret(),
                            stripeIntent.getId(),
                            stripeIntent.getStatus()
                    ),
                    "Payment intent created successfully"
            );

        } catch (StripeException ex) {
            throw new PaymentException("Failed to create payment intent: " + ex.getMessage());
        }
    }

    // ================= CONFIRM PAYMENT =================
    public ApiResponse<PaymentResponse> confirmPayment(Long userId, ConfirmPaymentRequest request) {
        try {
            Payment payment = findPaymentForUser(request.paymentIntentId(), userId);

            PaymentIntent confirmedIntent = stripeService.confirmPaymentIntent(
                    request.paymentIntentId(), request.paymentMethodId()
            );

            // ✅ Only update DB
            payment.setStatus(confirmedIntent.getStatus());
            paymentRepository.save(payment);

            return new ApiResponse<>(mapToResponse(payment), "Payment confirmation initiated");

        } catch (StripeException ex) {
            throw new PaymentException("Payment confirmation failed: " + ex.getMessage());
        }
    }

    // ================= REFUND PAYMENT =================
    public ApiResponse<PaymentResponse> refundPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentByUser(paymentId, userId);

        if (!STATUS_SUCCEEDED.equals(payment.getStatus())) {
            throw new PaymentException("Can only refund succeeded payments");
        }

        try {
            stripeService.retrievePaymentIntent(payment.getStripePaymentIntentId());
            payment.setStatus(STATUS_REFUNDED);
            paymentRepository.save(payment);

            return new ApiResponse<>(mapToResponse(payment), "Payment refunded successfully");
        } catch (StripeException ex) {
            throw new PaymentException("Refund failed: " + ex.getMessage());
        }
    }

    // ================= GET PAYMENT =================
    public ApiResponse<PaymentResponse> getPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentByUser(paymentId, userId);
        return new ApiResponse<>(mapToResponse(payment), "Payment retrieved successfully");
    }

    // ================= HANDLE WEBHOOK EVENTS =================
    public void handlePaymentSuccess(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentException("Payment not found for Stripe ID " + stripePaymentIntentId));

        if (STATUS_SUCCEEDED.equals(payment.getStatus())) {
            logger.info("Payment already succeeded, skipping Stripe ID {}", stripePaymentIntentId);
            return;
        }

        payment.setStatus(STATUS_SUCCEEDED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentCompletedEvent(
                new PaymentCompletedEvent(
                        payment.getId(),
                        payment.getUserId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        System.currentTimeMillis()
                )
        );

        logger.info("Payment succeeded handled for Stripe ID {}", stripePaymentIntentId);
    }

    public void handlePaymentFailure(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new PaymentException("Payment not found for Stripe ID " + stripePaymentIntentId));

        if (STATUS_FAILED.equals(payment.getStatus())) {
            logger.info("Payment already failed, skipping Stripe ID {}", stripePaymentIntentId);
            return;
        }

        payment.setStatus(STATUS_FAILED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentFailedEvent(
                new PaymentFailedEvent(
                        payment.getId(),
                        payment.getUserId(),
                        payment.getOrderId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        "Webhook: payment failed",
                        "webhook_failure",
                        System.currentTimeMillis()
                )
        );

        logger.warn("Payment failed handled for Stripe ID {}", stripePaymentIntentId);
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

    private PaymentResponse mapToResponse(Payment payment) {
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