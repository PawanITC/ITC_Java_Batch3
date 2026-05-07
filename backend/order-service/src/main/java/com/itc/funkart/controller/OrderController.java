package com.itc.funkart.controller;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>Customer Order Controller</h2>
 * <p>
 * Provides authenticated users access to their specific order data.
 * </p>
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Retrieves specific order details, ensuring the requester is the owner.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserDto user) {

        // Pass userId to ensure the user can only see THEIR order
        OrderResponse response = orderService.getOrderByIdAndUserId(id, user.id());
        return ResponseEntity.ok(ApiResponse.success(response, "Order retrieved successfully"));
    }

    /**
     * Returns the full history for the current authenticated user.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersHistory(
            @AuthenticationPrincipal JwtUserDto user) {

        log.debug("Fetching order history for userId: {}", user.id());
        List<OrderResponse> history = orderService.getCustomerOrderHistory(user.id());
        return ResponseEntity.ok(ApiResponse.success(history, "Order history fetched"));
    }

    /**
     * Allows a customer to cancel their own order if it is in a cancellable state.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtUserDto user) {

        orderService.cancelOrder(id, user.id());
        return ResponseEntity.ok(ApiResponse.success(null, "Order cancellation successful"));
    }
}