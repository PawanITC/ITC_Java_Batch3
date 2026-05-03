package com.itc.funkart.service.impl;

import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.entity.OrderItem;
import com.itc.funkart.exception.OrderBadRequestException;
import com.itc.funkart.exception.OrderNotFoundException;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import com.itc.funkart.service.KafkaEventService;
import com.itc.funkart.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h2>OrderServiceImpl</h2>
 * <p>
 * Implementation of {@link OrderService} focused on high-throughput event processing.
 * Maps JPA entities to specialized Records to ensure <b>Method Area</b> efficiency.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final KafkaEventService eventService;

    @Override
    @Transactional
    @CircuitBreaker(name = "orderService")
    public OrderResponse processOrderInitiation(OrderInitiatedEvent event) {
        log.info("📥 Processing Initiation for User: {} | Amount: {}", event.userId(), event.totalAmount());

        Order order = mapper.toEntity(event);

        event.productIds().forEach(pid -> order.addOrderItem(OrderItem.builder()
                .productId(pid)
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(100.00))
                .build()));

        Order saved = repository.save(order);

        eventService.sendOrderEvent(saved, OrderEventType.ORDER_INITIATED);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = repository.findByIdAndCustomerIdWithItems(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or unauthorized"));

        if (!order.getStatus().isCancellable()) {
            throw new OrderBadRequestException("Order in state " + order.getStatus() + " cannot be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        eventService.sendOrderEvent(order, OrderEventType.ORDER_CANCELLED);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order ID " + id + " not found"));

        if (order.getStatus().isFinal()) {
            throw new OrderBadRequestException("Cannot modify terminal state: " + order.getStatus());
        }

        order.setStatus(newStatus);
        Order updated = repository.save(order);

        // 5. Routing Logic: Map Business Status to Kafka Event Type
        OrderEventType eventType = switch (newStatus) {
            case PENDING -> OrderEventType.ORDER_INITIATED;
            case PAID -> OrderEventType.PAYMENT_SUCCESS;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            case CANCELLED -> OrderEventType.ORDER_CANCELLED;
            case REFUNDED -> OrderEventType.ORDER_REFUNDED;
        };

        eventService.sendOrderEvent(updated, eventType);

        return mapper.toResponse(updated);
    }

    // --- Standard Retrieval Methods ---

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAndUserId(Long orderId, Long userId) {
        return repository.findByIdAndCustomerIdWithItems(orderId, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrderHistory(Long customerId) {
        return repository.findByCustomerIdWithItems(customerId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order ID " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return repository.findByStatus(status, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(Long customerId, OrderStatus status,
                                            LocalDateTime start, LocalDateTime end,
                                            Pageable pageable) {
        return repository.findByStatus(status, pageable).map(mapper::toResponse);
    }
}