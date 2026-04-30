package com.itc.funkart.controller;

import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.dto.jwt.JwtUserDto;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.response.ApiResponse;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h2>Order Management Controller</h2>
 * <p>
 * This controller provides RESTful endpoints for managing the lifecycle of customer orders.
 * It integrates with Spring Security to ensure users can only access their own data.
 * </p>
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;


    /**
     * <h3>Get Order Details</h3>
     * <p>Retrieves detailed information for a specific order by its primary key.</p>
     * * @param id The Database ID of the order.
     *
     * @return 200 OK with the {@link OrderResponse}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrder(id);
        return ResponseEntity.ok(new ApiResponse<>(response, "Order retrieved successfully"));
    }

    /**
     * <h3>Get Personal Order History</h3>
     * <p>Returns a list of all historical orders belonging to the authenticated user.</p>
     * * @param jwt The authenticated token containing the user's 'sub'.
     *
     * @return 200 OK with a list of {@link OrderResponse}.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersHistory(
            @AuthenticationPrincipal JwtUserDto user) {

        log.info("Fetching history for customerId: {}", user.id());
        List<OrderResponse> history = orderService.getCustomerOrderHistory(user.id());
        return ResponseEntity.ok(new ApiResponse<>(history, "Order history fetched"));
    }

    /**
     * <h3>Cancel Order</h3>
     * <p>Soft-cancels an order if it hasn't reached a final state.</p>
     * * @param id The ID of the order to cancel.
     *
     * @return 200 OK with a success message.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        orderService.updateOrderStatus(id, OrderStatus.CANCELLED);
        return ResponseEntity.ok(new ApiResponse<>(null, "Order cancellation request processed"));
    }
}