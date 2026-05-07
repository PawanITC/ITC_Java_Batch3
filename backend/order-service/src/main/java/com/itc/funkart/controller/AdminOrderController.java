package com.itc.funkart.controller;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.dto.OrderResponse;
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
 * <p>
 * Privileged operations for managing the order ecosystem.
 * </p>
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Retrieves a paginated list of orders, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Page<OrderResponse> orders = (status != null)
                ? orderService.getOrdersByStatus(status, pageable)
                : orderService.getAllOrders(pageable);

        return ResponseEntity.ok(ApiResponse.success(orders, "Admin order list retrieved"));
    }

    /**
     * Manually overrides an order status (e.g., to SHIPPED or DELIVERED).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus newStatus) {

        OrderResponse updated = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(ApiResponse.success(updated, "Order status updated to " + newStatus));
    }
}