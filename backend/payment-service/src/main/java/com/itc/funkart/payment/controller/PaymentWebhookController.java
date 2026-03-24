package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.service.PaymentService;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version}/payments")
public class PaymentWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public PaymentWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeEvent(@RequestBody String payload,
                                                    @RequestHeader("Stripe-Signature") String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            String eventType = event.getType();

            event.getDataObjectDeserializer().getObject().ifPresent(obj -> {
                if (obj instanceof PaymentIntent intent) {
                    switch (eventType) {
                        case "payment_intent.succeeded" -> paymentService.handlePaymentSuccess(intent.getId());
                        case "payment_intent.payment_failed" -> paymentService.handlePaymentFailure(intent.getId());
                        default -> logger.info("Unhandled Stripe event: {}", eventType);
                    }
                }
            });

            return ResponseEntity.ok("Webhook processed");

        } catch (SignatureVerificationException ex) {
            logger.error("Invalid Stripe signature: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        } catch (Exception ex) {
            logger.error("Error processing webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing error");
        }
    }
}