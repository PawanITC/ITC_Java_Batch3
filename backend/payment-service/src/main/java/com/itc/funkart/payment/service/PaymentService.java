package com.itc.funkart.payment.service;

import com.itc.funkart.common.dto.event.payment.PaymentCompletedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentFailedEvent;
import com.itc.funkart.common.dto.event.payment.PaymentRefundedEvent;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final String STATUS_SUCCEEDED = "succeeded";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_REFUNDED = "refunded";
    private static final String STATUS_PROCESSING = "PROCESSING";

    private final PaymentRepository paymentRepository;
    private final StripeService stripeService;
    private final KafkaEventPublisher kafkaEventPublisher;

    // -----------------------------
    // CREATE PAYMENT INTENT (IDEMPOTENT)
    // -----------------------------
    @Transactional
    public PaymentIntentResponse createPaymentIntent(JwtUserDto user, CreatePaymentIntentRequest request) {
        try {
            Payment payment = paymentRepository.findByOrderId(request.orderId())
                    .orElseGet(() -> paymentRepository.save(
                            new Payment(user.id(), request.orderId(), request.amount(), request.currency())
                    ));

            if (payment.getStripePaymentIntentId() != null) {
                log.info("♻️ Reusing existing PaymentIntent for orderId={}", request.orderId());
                PaymentIntent existing = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
                return PaymentIntentResponse.from(existing);
            }

            PaymentIntent stripeIntent = stripeService.createPaymentIntent(
                    request.amount(),
                    request.currency(),
                    user.id(),
                    payment.getId(),
                    payment.getOrderId()
            );

            payment.setStripePaymentIntentId(stripeIntent.getId());
            paymentRepository.save(payment);

            return PaymentIntentResponse.from(stripeIntent);

        } catch (Exception ex) {
            log.error("Failed to create intent for Order: {}", request.orderId(), ex);
            throw new PaymentException("Intent creation failed: " + ex.getMessage());
        }
    }

    // -----------------------------
    // CONFIRM PAYMENT (TRIGGER ONLY - NOT SOURCE OF TRUTH)
    // -----------------------------
    @Transactional
    public PaymentResponse confirmPayment(JwtUserDto user, ConfirmPaymentRequest request) {
        try {
            Payment payment = findPaymentForUser(request.paymentIntentId(), user);

            // 🔐 STRICT GUARD: NEVER override final states
            if (isFinalState(payment.getStatus())) {
                log.info("♻️ Payment already finalized: {}", payment.getId());
                return PaymentResponse.from(payment);
            }

            if (STATUS_PROCESSING.equals(payment.getStatus())) {
                log.info("♻️ Payment already processing: {}", payment.getId());
                return PaymentResponse.from(payment);
            }

            stripeService.confirmPaymentIntent(
                    request.paymentIntentId(),
                    request.paymentMethodId(),
                    request.returnUrl(),
                    payment.getId()
            );

            // ONLY provisional state
            payment.setStatus(STATUS_PROCESSING);
            paymentRepository.save(payment);

            return PaymentResponse.from(payment);

        } catch (StripeException ex) {
            log.error("Stripe confirmation failed for Intent: {}", request.paymentIntentId(), ex);
            throw new PaymentException("Confirmation failed: " + ex.getMessage());
        }
    }

    // -----------------------------
    // REFUND (IDEMPOTENT)
    // -----------------------------
    @Transactional
    public PaymentResponse refundPayment(JwtUserDto user, Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            if (!user.id().equals(payment.getUserId()))
                throw new PaymentException("Unauthorized");

            if (STATUS_REFUNDED.equals(payment.getStatus())) {
                log.info("♻️ Already refunded paymentId={}", paymentId);
                return PaymentResponse.from(payment);
            }

            if (!STATUS_SUCCEEDED.equalsIgnoreCase(payment.getStatus())) {
                throw new PaymentException("Only succeeded payments can be refunded.");
            }

            Refund stripeRefund = stripeService.refundPayment(
                    payment.getStripePaymentIntentId(),
                    paymentId
            );

            payment.setStatus(STATUS_REFUNDED);
            paymentRepository.save(payment);

            kafkaEventPublisher.publishPaymentRefundedEvent(PaymentRefundedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .stripeRefundId(stripeRefund.getId())
                    .amountRefunded(stripeRefund.getAmount())
                    .currency(payment.getCurrency())
                    .timestamp(Instant.now().toEpochMilli())
                    .build());

            return PaymentResponse.from(payment);

        } catch (StripeException ex) {
            throw new PaymentException("Stripe refund failed: " + ex.getMessage());
        }
    }


    public PaymentResponse getPayment(JwtUserDto user, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found"));

        if (!user.id().equals(payment.getUserId())) {
            throw new PaymentException("Unauthorized access to payment");
        }

        return PaymentResponse.from(payment);
    }

    // -----------------------------
    // GET LATEST PAYMENT INTENT (for checkout page restore)
    // -----------------------------
    public PaymentIntentResponse getLatestPaymentIntent(JwtUserDto user) {
        try {
            Payment payment = paymentRepository.findTopByUserIdOrderByCreatedAtDesc(user.id())
                    .orElseThrow(() -> new PaymentException("No payment found for user"));

            if (payment.getStripePaymentIntentId() == null) {
                throw new PaymentException("Payment intent not yet created");
            }

            PaymentIntent intent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
            return PaymentIntentResponse.from(intent);
        } catch (com.stripe.exception.StripeException ex) {
            throw new PaymentException("Could not retrieve payment intent: " + ex.getMessage());
        }
    }

    // -----------------------------
    // WEBHOOK (SOURCE OF TRUTH)
    // -----------------------------
    public void handlePaymentSuccess(PaymentIntent intent) {
        Payment payment = getInternalPayment(intent.getId());

        if (payment.getStatus() != null && payment.getStatus().equals(STATUS_SUCCEEDED)) {
            log.info("♻️ Already succeeded payment={}", payment.getId());
            return;
        }

        // ONLY upgrade allowed
        payment.setStatus(STATUS_SUCCEEDED);
        paymentRepository.save(payment);

        kafkaEventPublisher.publishPaymentCompletedEvent(PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .stripeId(intent.getId())
                .amount(intent.getAmountReceived())
                .timestamp(Instant.now().toEpochMilli())
                .build());
    }

    public void handlePaymentFailure(PaymentIntent intent) {
        Payment payment = getInternalPayment(intent.getId());

        if (STATUS_SUCCEEDED.equals(payment.getStatus())) {
            log.warn("⚠️ Ignoring failure after success (race condition)");
            return;
        }

        if (STATUS_FAILED.equals(payment.getStatus())) return;

        payment.setStatus(STATUS_FAILED);
        paymentRepository.save(payment);

        var error = intent.getLastPaymentError();

        kafkaEventPublisher.publishPaymentFailedEvent(PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .stripeId(intent.getId())
                .errorMessage(error != null ? error.getMessage() : "Payment declined")
                .stripeErrorCode(error != null ? error.getCode() : "unknown")
                .timestamp(Instant.now().toEpochMilli())
                .build());
    }

    public void handlePaymentRefunded(Charge charge) {
        Payment payment = getInternalPayment(charge.getPaymentIntent());

        if (STATUS_REFUNDED.equals(payment.getStatus())) return;

        payment.setStatus(STATUS_REFUNDED);
        paymentRepository.save(payment);

        String refundId = (charge.getRefunds() != null && !charge.getRefunds().getData().isEmpty())
                ? charge.getRefunds().getData().get(0).getId()
                : "re_webhook";

        kafkaEventPublisher.publishPaymentRefundedEvent(PaymentRefundedEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .stripeRefundId(refundId)
                .amountRefunded(charge.getAmountRefunded())
                .currency(payment.getCurrency())
                .timestamp(Instant.now().toEpochMilli())
                .build());
    }


    @Transactional
    public void processWebhookEvent(Event event) {

        log.debug("Processing Webhook Event | ID: {} | Type: {}", event.getId(), event.getType());

        StripeObject stripeObject;
        try {
            stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseGet(() -> {
                        try {
                            return event.getDataObjectDeserializer().deserializeUnsafe();
                        } catch (StripeException e) {
                            throw new PaymentException("Failed to deserialize Stripe event: " + event.getId());
                        }
                    });
        } catch (Exception ex) {
            throw new PaymentException("Webhook deserialization failure: " + event.getId());
        }

        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                if (stripeObject instanceof PaymentIntent intent) {
                    handlePaymentSuccess(intent);
                }
            }

            case "payment_intent.payment_failed" -> {
                if (stripeObject instanceof PaymentIntent intent) {
                    handlePaymentFailure(intent);
                }
            }

            case "charge.refunded" -> {
                if (stripeObject instanceof Charge charge) {
                    handlePaymentRefunded(charge);
                }
            }

            default -> log.info("ℹ️ Unhandled event type: {}", event.getType());
        }
    }

    // -----------------------------
    // HELPERS
    // -----------------------------
    private boolean isFinalState(String status) {
        return STATUS_SUCCEEDED.equals(status)
                || STATUS_FAILED.equals(status)
                || STATUS_REFUNDED.equals(status);
    }

    private Payment getInternalPayment(String stripeId) {
        return paymentRepository.findByStripePaymentIntentId(stripeId)
                .orElseThrow(() -> new PaymentException("Local payment record not found for: " + stripeId));
    }

    private Payment findPaymentForUser(String stripeId, JwtUserDto user) {
        Payment payment = getInternalPayment(stripeId);
        if (!user.id().equals(payment.getUserId())) {
            throw new PaymentException("Unauthorized access to payment intent");
        }
        return payment;
    }
}