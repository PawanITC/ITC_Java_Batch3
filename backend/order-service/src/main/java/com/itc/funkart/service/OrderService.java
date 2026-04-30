package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>Order Service</h2>
 * <p>Coordinates the lifecycle of customer orders, including creation from cart,
 * status updates, and historical retrieval.</p>
 */
public interface OrderService {

    OrderResponse createOrder(OrderRequest request, Long customerId);

    OrderResponse getOrder(Long id);

    List<OrderResponse> getCustomerOrderHistory(Long customerId);

    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);

    /**
     * <h3>Admin Search</h3>
     * <p>Provides advanced filtering and pagination for store management.</p>
     *
     * @param customerId Optional filter by user.
     * @param status     Optional filter by order state.
     * @param start      Filter orders after this date.
     * @param end        Filter orders before this date.
     * @param pageable   Pagination and sorting details.
     * @return A paginated slice of order data.
     */
    Page<OrderResponse> searchOrders(Long customerId, OrderStatus status,
                                     LocalDateTime start, LocalDateTime end,
                                     Pageable pageable);

    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    Page<OrderResponse> getAllOrders(Pageable pageable);
}