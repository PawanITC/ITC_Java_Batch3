package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.dto.webhook.PaymentIntentWebhookDto;
import com.itc.funkart.payment.dto.webhook.PaymentIntentMapper;
import com.itc.funkart.payment.service.PaymentService;

import com.stripe.exception.SignatureVerificationException;
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
            // ================= VERIFY SIGNATURE =================
            Webhook.constructEvent(payload, signature, webhookSecret);

            // ================= MAP PAYLOAD =================
            PaymentIntentWebhookDto dto = PaymentIntentMapper.fromJson(payload);

            String status = dto.status();
            if (status == null) {
                logger.warn("Webhook received with null status for payment intent {}", dto.id());
                return ResponseEntity.ok("Ignored webhook with null status");
            }

            // ================= HANDLE EVENT =================
            switch (status) {
                case "succeeded" -> paymentService.handlePaymentSuccess(dto.id());
                case "failed" -> paymentService.handlePaymentFailure(dto.id());
                default -> logger.info("Unhandled payment status: {}", status);
            }

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