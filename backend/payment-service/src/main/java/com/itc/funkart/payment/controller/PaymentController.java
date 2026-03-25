package com.itc.funkart.payment.controller;

import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.dto.request.CreatePaymentIntentRequest;
import com.itc.funkart.payment.response.ApiResponse;
import com.itc.funkart.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version}/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-intent")
    public ResponseEntity<ApiResponse<?>> createPaymentIntent(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody CreatePaymentIntentRequest request) {

        logger.info("User {} requested to create payment intent", user.id());
        ApiResponse<?> response = paymentService.createPaymentIntent(user.id(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<?>> confirmPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @Valid @RequestBody ConfirmPaymentRequest request) {

        logger.info("User {} requested to confirm payment {}", user.id(), request.paymentIntentId());
        ApiResponse<?> response = paymentService.confirmPayment(user.id(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<?>> getPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        logger.info("User {} requested payment {}", user.id(), paymentId);
        ApiResponse<?> response = paymentService.getPayment(user.id(), paymentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<?>> refundPayment(
            @AuthenticationPrincipal JwtUserDto user,
            @PathVariable Long paymentId) {

        logger.info("User {} requested refund for payment {}", user.id(), paymentId);
        ApiResponse<?> response = paymentService.refundPayment(user.id(), paymentId);
        return ResponseEntity.ok(response);
    }
}