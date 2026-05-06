package com.itc.funkart.service.impl;

import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
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

import java.time.LocalDateTime;
import java.util.List;

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
    public OrderResponse processOrderInitiation(CheckoutInitiatedEvent event) {
        log.info("📥 Processing Initiation for User: {} | Amount: {}", event.customerId(), event.totalAmount());

        Order order = mapper.toEntity(event);

        event.items().forEach(
                itemPayload -> order.addOrderItem(OrderItem.builder()
                        .productId(itemPayload.productId())
                        .quantity(itemPayload.quantity())
                        .priceAtPurchase(itemPayload.price())
                        .build()));

        Order saved = repository.saveAndFlush(order);
        log.info("✅ Order {} persisted for Customer {}", saved.getId(), saved.getCustomerId());

        eventService.sendOrderCreated(saved);

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
        repository.saveAndFlush(order);

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
        Order updated = repository.saveAndFlush(order);

        // FIX: removed case CONFIRMED -> ORDER_CONFIRMED — neither exists in your enums.
        // If you later add CONFIRMED to OrderStatus and ORDER_CONFIRMED to OrderEventType,
        // add the case back at that point.
        OrderEventType eventType = switch (updated.getStatus()) {
            case PENDING -> OrderEventType.ORDER_INITIATED;
            case PAID -> OrderEventType.PAYMENT_SUCCESS;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            case CONFIRMED -> OrderEventType.ORDER_CONFIRMED;
            case CANCELLED -> OrderEventType.ORDER_CANCELLED;
            case FAILED -> OrderEventType.PAYMENT_FAILED;
            case REFUNDED -> OrderEventType.ORDER_REFUNDED;
        };

        eventService.sendOrderEvent(updated, eventType);

        return mapper.toResponse(updated);
    }

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