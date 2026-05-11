package com.itc.funkart.payment.controller;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.dto.response.PaymentIntentResponse;
import com.itc.funkart.payment.dto.response.PaymentResponse;
import com.itc.funkart.payment.exception.PaymentException;
import com.itc.funkart.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>PaymentController</h2>
 * <p>
 * REST entry point for the Payment Microservice. This controller handles the
 * handoff between the Security Context (JWT) and the Payment Business Logic.
 * </p>
 * <p>
 * <b>Architectural Note:</b> The Service layer already returns the
 * {@link ApiResponse} envelope. The Controller's job is simply to
 * wrap that envelope in a {@link ResponseEntity} to set the HTTP status codes.
 * </p>
 */
@RestController
@RequestMapping("/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates a Stripe PaymentIntent and persists the local Payment record.
     *
     * @param user    The authenticated user from the Security Context.
     * @param request Contains the Order ID and Amount.
     * @return 200 OK with the client secret for the Stripe Elements frontend.
     */
    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {

        log.info("→ Creating Intent | User: {} | Order: {}", user.id(), request.orderId());
        PaymentIntentResponse data = paymentService.createPaymentIntent(user, request);

        if (user.id() == null) {
            throw new PaymentException("Unauthorized: Missing user security context");
        }

        return ResponseEntity.ok(ApiResponse.success(data, "Payment intent created successfully"));
    }

    /**
     * Confirms a payment on the server-side if manual confirmation is required.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        log.info("→ Confirming Payment | User: {} | Intent: {}", user.id(), request.paymentIntentId());
        PaymentResponse data = paymentService.confirmPayment(user, request);

        return ResponseEntity.ok(ApiResponse.success(data, "Payment confirmed and processed"));
    }

    /**
     * Retrieves the most recent PaymentIntent for the authenticated user.
     * Called by the checkout page to restore an in-flight payment after navigation
     * (e.g. browser back, page refresh) when no checkoutResult is in router state.
     *
     * @param user The authenticated user from the Security Context.
     * @return 200 OK with clientSecret and paymentIntentId for Stripe Elements.
     */
    @GetMapping("/my-latest")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> getLatestPaymentIntent(
            @AuthenticationPrincipal JwtUserDto user) {

        log.debug("→ Fetching latest PaymentIntent | User: {}", user.id());
        java.util.Optional<PaymentIntentResponse> data = paymentService.getLatestPaymentIntent(user);

        // Return 204 (No Content) when payment intent is not yet ready.
        // The checkout page polls this endpoint — 204 means "keep waiting", not an error.
        if (data.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ApiResponse.success(data.get(), "Latest payment intent retrieved"));
    }

    /**
     * Retrieves specific payment details.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        log.debug("→ Fetching Payment | ID: {}", paymentId);
        PaymentResponse data = paymentService.getPayment(user, paymentId);

        return ResponseEntity.ok(ApiResponse.success(data, "Payment details retrieved"));
    }

    /**
     * Initiates a refund workflow for a successful payment.
     *
     * @param user      The authenticated principal from the Security Context.
     * @param paymentId The internal ID of the payment to be reversed.
     * @return 200 OK with the updated Payment details (status: REFUNDED).
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        log.warn("→ Refund Request | User: {} | Payment: {}", user.id(), paymentId);

        // Call service to get raw DTO
        PaymentResponse data = paymentService.refundPayment(user, paymentId);

        // Wrap in the Unified Response Envelope
        return ResponseEntity.ok(ApiResponse.success(data, "Refund successfully initiated"));
    }
}