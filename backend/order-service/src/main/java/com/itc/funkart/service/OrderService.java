package com.itc.funkart.service;

import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>OrderService</h2>
 * <p>
 * The central business authority for order lifecycles within the FunKart ecosystem.
 * This service handles the transition from asynchronous intent (Kafka events) to
 * persistent state (RDBMS).
 * </p>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><b>Asynchronous Inbound:</b> Processes initiation requests from the Cart service via Kafka.</li>
 *   <li><b>Transactional Integrity:</b> Ensures database consistency before broadcasting downstream events.</li>
 *   <li><b>Heap Efficiency:</b> Utilizes Java Records for outbound events to minimize GC overhead.</li>
 * </ul>
 */
public interface OrderService {

    /**
     * Processes an incoming order initiation from the checkout flow.
     * <p>
     * Converts a transient {@link OrderInitiatedEvent} into a persistent {@link Order} entity,
     * calculates totals, and triggers the full order broadcast.
     * </p>
     *
     * @param event The immutable record received from the message broker.
     * @return A mapped {@link OrderResponse} for immediate confirmation.
     */
    OrderResponse processOrderInitiation(OrderInitiatedEvent event);

    /**
     * Retrieves an order strictly associated with a specific user.
     *
     * @param orderId The primary key of the order.
     * @param userId  The owner's unique identifier.
     * @return The order details if found and authorized.
     */
    OrderResponse getOrderByIdAndUserId(Long orderId, Long userId);

    /**
     * Retrieves an order by ID (Internal/Admin use).
     */
    OrderResponse getOrder(Long id);

    /**
     * Fetches the complete order history for a specific customer.
     */
    List<OrderResponse> getCustomerOrderHistory(Long customerId);

    /**
     * Executes a cancellation request if the order state permits.
     * <p>
     * Triggers a specialized {@code OrderCancelledEvent} upon successful state transition.
     * </p>
     *
     * @param orderId The order to cancel.
     * @param userId  The user requesting the cancellation.
     */
    void cancelOrder(Long orderId, Long userId);

    /**
     * Updates the status of an existing order and broadcasts the relevant event.
     *
     * @param id        The order ID.
     * @param newStatus The target {@link OrderStatus}.
     * @return The updated order response.
     */
    OrderResponse updateOrderStatus(Long id, OrderStatus newStatus);

    /**
     * Advanced administrative search with multi-parameter filtering.
     */
    Page<OrderResponse> searchOrders(Long customerId, OrderStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Retrieves a paginated slice of orders by their current status.
     */
    Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable);

    /**
     * Retrieves all orders with pagination support.
     */
    Page<OrderResponse> getAllOrders(Pageable pageable);
}