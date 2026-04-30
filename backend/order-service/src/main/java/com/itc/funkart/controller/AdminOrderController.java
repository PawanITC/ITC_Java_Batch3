package com.itc.funkart.controller;

import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.response.ApiResponse;
import com.itc.funkart.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>Admin Order Controller</h2>
 * <p>Privileged endpoints for support and operations staff to manage all system orders.</p>
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * <h3>Search & Filter Orders</h3>
     * <p>Allows admins to browse orders with pagination and status filtering.</p>
     *
     * @param status   Optional filter for order state (PENDING, SHIPPED, etc.)
     * @param pageable Standard Spring Pageable (supports ?page=0&size=20&sort=createdAt,desc)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Page<OrderResponse> orders = (status != null)
                ? orderService.getOrdersByStatus(status, pageable)
                : orderService.getAllOrders(pageable);

        return ResponseEntity.ok(new ApiResponse<>(orders, "Admin order list retrieved"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus newStatus) {

        OrderResponse updated = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(new ApiResponse<>(updated, "Order status updated to " + newStatus));
    }
}