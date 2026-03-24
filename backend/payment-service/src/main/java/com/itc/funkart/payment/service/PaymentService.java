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

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          KafkaEventPublisher kafkaEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    // ================= CREATE PAYMENT INTENT =================
    public ApiResponse<PaymentIntentResponse> createPaymentIntent(Long userId, CreatePaymentIntentRequest request) {
        try {
            Payment payment = new Payment(userId, request.orderId(), request.amount(), request.currency());
            payment = paymentRepository.save(payment);

            PaymentIntent stripeIntent = StripeService.createPaymentIntentStatic(
                    request.amount(), request.currency(), userId, payment.getId()
            );

            payment.setStripePaymentIntentId(stripeIntent.getId());
            paymentRepository.save(payment);

            return new ApiResponse<>(new PaymentIntentResponse(
                    stripeIntent.getClientSecret(),
                    stripeIntent.getId(),
                    stripeIntent.getStatus()
            ), "Payment intent created successfully");

        } catch (StripeException ex) {
            throw new PaymentException("Failed to create payment intent: " + ex.getMessage());
        }
    }

    // ================= CONFIRM PAYMENT =================
    public ApiResponse<PaymentResponse> confirmPayment(Long userId, ConfirmPaymentRequest request) {
        try {
            Payment payment = findPaymentForUser(request.paymentIntentId(), userId);
            PaymentIntent confirmedIntent = StripeService.confirmPaymentIntentStatic(
                    request.paymentIntentId(), request.paymentMethodId()
            );

            payment.setStatus(confirmedIntent.getStatus());
            paymentRepository.save(payment);
            publishKafkaEvent(payment, confirmedIntent);

            return new ApiResponse<>(mapToResponse(payment), "Payment confirmed successfully");

        } catch (StripeException ex) {
            publishPaymentFailedEvent(request.paymentIntentId(), userId, ex);
            throw new PaymentException("Payment confirmation failed: " + ex.getMessage());
        }
    }

    // ================= REFUND PAYMENT =================
    public ApiResponse<PaymentResponse> refundPayment(Long userId, Long paymentId) {
        Payment payment = findPaymentByUser(paymentId, userId);
        if (!"succeeded".equals(payment.getStatus())) {
            throw new PaymentException("Can only refund succeeded payments");
        }
        try {
            StripeService.refundPaymentStatic(payment.getStripePaymentIntentId());
            payment.setStatus("refunded");
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

        payment.setStatus("succeeded");
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

        payment.setStatus("failed");
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

    private void publishKafkaEvent(Payment payment, PaymentIntent intent) {
        String status = intent.getStatus();
        if ("succeeded".equals(status)) {
            kafkaEventPublisher.publishPaymentCompletedEvent(
                    new PaymentCompletedEvent(payment.getId(), payment.getUserId(), payment.getOrderId(),
                            payment.getAmount(), payment.getCurrency(), System.currentTimeMillis())
            );
        } else if (!"requires_action".equals(status)) {
            kafkaEventPublisher.publishPaymentFailedEvent(
                    new PaymentFailedEvent(payment.getId(), payment.getUserId(), payment.getOrderId(),
                            payment.getAmount(), payment.getCurrency(),
                            "Payment declined or failed",
                            intent.getLastPaymentError() != null ? intent.getLastPaymentError().getCode() : "unknown",
                            System.currentTimeMillis())
            );
        }
    }

    private void publishPaymentFailedEvent(String stripePaymentIntentId, Long userId, StripeException ex) {
        paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .ifPresent(payment -> kafkaEventPublisher.publishPaymentFailedEvent(
                        new PaymentFailedEvent(payment.getId(), payment.getUserId(), payment.getOrderId(),
                                payment.getAmount(), payment.getCurrency(),
                                ex.getMessage(), ex.getCode(), System.currentTimeMillis())
                ));
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getUserId(), payment.getOrderId(),
                payment.getAmount(), payment.getCurrency(), payment.getStatus(), payment.getCreatedAt());
    }
}