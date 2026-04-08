package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.response.ApiResponse;
import com.itc.funkart.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for FunKart Payment Operations.
 * * <p><b>Security:</b> All endpoints in this controller require a valid
 * JWT Bearer Token. The user's identity is resolved automatically
 * via {@link JwtUserDto}.</p>
 * * <p><b>Version Control:</b> Uses the <code>${api.version}</code> property
 * to maintain flexibility across deployment environments.</p>
 * * <p><b>Core Flow:</b>
 * 1. Client calls /create-intent to get Stripe Secret.
 * 2. Client completes payment on Frontend via Stripe Elements.
 * 3. Client calls /confirm to sync final status with Backend.
 * 4. (Optional) Admin/User calls /{id}/refund to reverse funds. ↺
 * </p>
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {

        // Use info for the high-level intent
        logger.info("Payment Intent Creation started | User: {} | Order: {}", user.id(), request.orderId());

        ApiResponse<PaymentIntentResponse> response = paymentService.createPaymentIntent(user.id(), request);

        // Log the success (and the Stripe PI ID if available)
        logger.info("Payment Intent Created | User: {} | PI: {}", user.id(), response.getData().paymentIntentId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody ConfirmPaymentRequest request) {


        logger.info("Payment Confirmation initiated | User: {} | PI: {}", user.id(), request.paymentIntentId());

        ApiResponse<PaymentResponse> response = paymentService.confirmPayment(user.id(), request);


        logger.info("Payment Confirmation result: {} | User: {} | PI: {}",
                response.getData().status(), user.id(), request.paymentIntentId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        // 1. Log the incoming "Intent"
        logger.info("Fetch Payment Data | User: {} | PaymentID: {}", user.id(), paymentId);

        ApiResponse<PaymentResponse> response = paymentService.getPayment(user.id(), paymentId);

        // 2. Log the "Outcome" (Optional for GET, but helpful for debugging permissions)
        logger.info("Payment Fetched Successfully | User: {} | Status: {}",
                user.id(), response.getData().status());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        logger.info("Refund requested | User: {} | PaymentID: {}", user.id(), paymentId);

        ApiResponse<PaymentResponse> response = paymentService.refundPayment(user.id(), paymentId);

        logger.info("Refund processed successfully | User: {} | PaymentID: {} | Amount: {}",
                user.id(), paymentId, response.getData().amount());

        return ResponseEntity.ok(response);
    }
}