package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.dto.webhook.StripeWebhookResponse;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>PaymentWebhookController</h2>
 * <p>
 * The public-facing gateway for asynchronous notifications (webhooks) from Stripe.
 * </p>
 * <p>
 * Unlike standard endpoints, this controller does not use JWT authentication. Instead,
 * it relies on <b>Stripe Signature Verification</b> to ensure that incoming payloads
 * are authentic and have not been tampered with in transit.
 * </p>
 *
 * @author Abbas
 * @version 1.2
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
     * Entry point for Stripe's event notification system.
     * <p>
     * This method handles the cryptographic verification of the signature header and
     * routes the event to the appropriate business logic in the service layer.
     * </p>
     *
     * @param payload   The raw JSON string from the request body.
     * @param sigHeader The {@code Stripe-Signature} header provided by Stripe.
     * @return A {@link ResponseEntity} containing a success or failure status for Stripe to log.
     */
    @PostMapping("/webhook")
    public ResponseEntity<StripeWebhookResponse> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        if (sigHeader == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StripeWebhookResponse("Bad Request", "Missing Stripe-Signature header"));
        }

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.debug("Stripe Webhook Verified | Event ID: {}", event.getId());

            // 2. Delegate to service (Which throws PaymentException if object is empty)
            paymentService.processWebhookEvent(event);

            return ResponseEntity.ok(StripeWebhookResponse.builder()
                    .status("Success")
                    .message("Event processed: " + event.getType())
                    .build());

        } catch (SignatureVerificationException ex) {
            logger.error("🛡️ Security Alert: Invalid Webhook Signature!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new StripeWebhookResponse("Unauthorized", "Invalid Stripe Signature"));
        } catch (PaymentException ex) {
            // NEW: Specific catch for your annotated exception
            logger.warn("⚠️ Webhook Validation Failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new StripeWebhookResponse("Bad Request", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("🔥 Webhook System Error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StripeWebhookResponse("Error", "Internal system error occurred"));
        }
    }
}