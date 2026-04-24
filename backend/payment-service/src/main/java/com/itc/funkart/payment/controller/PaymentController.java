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
 * <h2>PaymentController</h2>
 * <p>
 * Orchestrates user-initiated payment actions. This controller is the primary
 * interface for the frontend application to interact with the payment lifecycle.
 * </p>
 * <p>
 * All endpoints are secured and require a valid {@link JwtUserDto} resolved via
 * {@code @AuthenticationPrincipal} to ensure data isolation and security.
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

    /**
     * Initializes a new payment lifecycle by creating a Stripe PaymentIntent.
     *
     * @param user    The authenticated user principal.
     * @param request The amount, currency, and order metadata.
     * @return The Stripe client secret required for frontend card collection.
     */
    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {

        logger.info("Payment Intent Creation started | User: {} | Order: {}", user.id(), request.orderId());
        ApiResponse<PaymentIntentResponse> response = paymentService.createPaymentIntent(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Triggers server-side confirmation for payments requiring manual finalization.
     *
     * @param user    The authenticated user principal.
     * @param request The specific PaymentIntent and PaymentMethod IDs.
     * @return The updated status of the payment.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        logger.info("Payment Confirmation initiated | User: {} | PI: {}", user.id(), request.paymentIntentId());
        ApiResponse<PaymentResponse> response = paymentService.confirmPayment(user, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches the current status and metadata of a payment from the local repository.
     *
     * @param user      The authenticated user principal.
     * @param paymentId The internal database ID for the payment record.
     * @return Detailed payment metadata including transaction status.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        ApiResponse<PaymentResponse> response = paymentService.getPayment(user, paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Requests a fund reversal for a previously successful transaction.
     *
     * @param user      The authenticated user principal.
     * @param paymentId The internal database ID of the payment to refund.
     * @return The updated payment record reflecting the 'REFUNDED' status.
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        logger.info("Refund requested | User: {} | PaymentID: {}", user.id(), paymentId);
        ApiResponse<PaymentResponse> response = paymentService.refundPayment(user, paymentId);
        return ResponseEntity.ok(response);
    }


}