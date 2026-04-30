package com.itc.funkart.service.impl;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.exception.OrderBadRequestException;
import com.itc.funkart.exception.OrderNotFoundException;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import com.itc.funkart.service.KafkaEventService;
import com.itc.funkart.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>Order Service Implementation</h2>
 * <p>
 * Manages the order lifecycle, leveraging {@link KafkaEventService} for asynchronous
 * downstream communication and {@link OrderMapper} for clean DTO transformations.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final KafkaEventService eventService; // Decoupled messaging service

    @Override
    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")
    public OrderResponse createOrder(OrderRequest request, Long customerId) {
        log.info("🚀 Initiating order creation for customer: {}", customerId);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderBadRequestException("Order must contain at least one item.");
        }

        // 1. Map Request DTO to Entity (Manual Mapper handles child item linking)
        Order order = mapper.toEntity(request);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);

        // 2. Hydrate Item Prices (Simulation of Product Service check)
        // In Prod, you'd verify these IDs and prices via an internal Feign client
        order.getItems().forEach(item -> item.setPriceAtPurchase(BigDecimal.valueOf(100.00)));

        // 3. Financial Calculation
        order.setTotalAmount(calculateTotal(order.getItems()));

        // 4. Persistence
        Order saved = repository.save(order);
        log.info("✅ Order persisted: ID={}", saved.getId());

        // 5. Asynchronous Event Broadcasting via Messaging Service
        boolean published = eventService.sendOrderEvent(saved);

        // 6. Build Response
        OrderResponse response = mapper.toResponse(saved);
        response.setEventStatus(published ? "PUBLISHED" : "PENDING_RETRY");

        return response;
    }

    @Override
    public OrderResponse getOrder(Long id) {
        log.debug("Fetching order: {}", id);
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public List<OrderResponse> getCustomerOrderHistory(Long customerId) {
        log.info("Retrieving history for customer: {}", customerId);
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        // Business Rule: Guard against updating terminal states
        if (order.getStatus().isFinal()) {
            throw new OrderBadRequestException(
                    "State Transition Forbidden: Order " + id + " is already " + order.getStatus());
        }

        log.info("🔄 Order {}: Status changing from {} to {}", id, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        Order updated = repository.save(order);

        // Broadcast change so Inventory/Shipping services can react
        eventService.sendOrderEvent(updated);

        return mapper.toResponse(updated);
    }

    @Override
    public Page<OrderResponse> searchOrders(Long customerId, OrderStatus status,
                                            LocalDateTime start, LocalDateTime end,
                                            Pageable pageable) {
        log.info("🔍 Admin searching orders. Filter [Status: {}]", status);

        // Simple conditional filtering. For more complex logic, use Specifications.
        Page<Order> orders = (status != null)
                ? repository.findByStatus(status, pageable)
                : repository.findAll(pageable);

        return orders.map(mapper::toResponse);
    }

    @Override
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {

        Page<Order> orders = repository.findByStatus(status, pageable);

        return orders.map(mapper::toResponse);
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    // --- Private Helper Methods ---

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Resilience4j Fallback logic for createOrder.
     */
    public OrderResponse fallbackCreateOrder(OrderRequest request, Long customerId, Throwable t) {
        log.error("🚨 Circuit Breaker [createOrder] active for user {}. Reason: {}", customerId, t.getMessage());
        return OrderResponse.builder()
                .orderStatus(OrderStatus.PENDING)
                .eventStatus("SERVICE_BUSY_RETRY_LATER")
                .build();
    }
}