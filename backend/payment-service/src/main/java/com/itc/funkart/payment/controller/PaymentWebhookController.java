package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.dto.webhook.StripeWebhookResponse;
import com.itc.funkart.payment.service.PaymentService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Webhook Listener for Stripe Events.
 * <p>
 * This controller handles asynchronous updates from Stripe. It uses the
 * Stripe SDK to verify that incoming requests actually originated from Stripe.
 * </p>
 */
@RestController
@RequestMapping("/payments")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Entry point for Stripe async notifications.
     * Path: /api/v1/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<StripeWebhookResponse> handleWebhook(@RequestBody String payload,
                                                               @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {

            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.debug("Stripe Webhook Verified | Event ID: {}", event.getId());

            StripeObject stripeObject = event.getDataObjectDeserializer().getObject()
                    .orElseGet(() -> {
                        try {
                            return event.getDataObjectDeserializer().deserializeUnsafe();
                        } catch (EventDataObjectDeserializationException e) {
                            throw new RuntimeException("Deserialization failed", e);
                        }
                    });

            if (stripeObject == null) {
                logger.error("❌ Deserialization failed for event: {}", event.getId());
                return ResponseEntity.badRequest()
                        .body(new StripeWebhookResponse("Error", "Payload extraction failed"));
            }

            switch (event.getType()) {
                case "payment_intent.succeeded" -> {
                    PaymentIntent pi = (PaymentIntent) stripeObject;
                    logger.info("💳 Payment Succeeded | ID: {} | Amount: {}", pi.getId(), pi.getAmount());
                    paymentService.handlePaymentSuccess(pi);
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = (PaymentIntent) stripeObject;
                    logger.warn("⚠️ Payment Failed | ID: {} | Reason: {}",
                            pi.getId(), pi.getLastPaymentError().getMessage());
                    paymentService.handlePaymentFailure(pi);
                }
                default -> logger.trace("Ignored event type: {}", event.getType());
            }

            return ResponseEntity.ok(StripeWebhookResponse.builder()
                    .status("Success")
                    .message("Event processed: " + event.getType())
                    .build());

        } catch (SignatureVerificationException ex) {
            logger.error("🛡️ Security Alert: Invalid Webhook Signature!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new StripeWebhookResponse("Unauthorized", "Invalid Stripe Signature"));
        } catch (Exception ex) {
            logger.error("🔥 Webhook System Error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeWebhookResponse("Error", "Internal system error occurred"));
        }
    }
}