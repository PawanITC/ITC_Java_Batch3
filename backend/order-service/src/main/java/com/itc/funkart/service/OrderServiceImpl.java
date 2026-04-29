
package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.exception.OrderNotFound;
import com.itc.funkart.kafka.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final OrderEventProducer producer;

    // ================= Create Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackCreateOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    @CacheEvict(value = "allOrders", allEntries = true)
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // === NEW: Generate Correlation ID for tracing ===
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("Creating order for customerId={} and productId={}",
                    request.getCustomerId(), request.getProductId());

            UUID customerId = parseUUID(request.getCustomerId());
            UUID productId = parseUUID(request.getProductId());

            Order order = mapper.toEntity(request);
            order.setCustomerId(customerId);
            order.setProductId(productId);
            order.setOrderStatus("CREATED");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            order.setCorrelationId(correlationId);  // === NEW ===

            Order saved = repository.save(order);

            // === NEW: Pass correlationId to producer ===
            boolean eventPublished = sendKafkaEvent(
                    () -> producer.publishOrderCreated(saved, correlationId),
                    saved.getOrderId(),
                    correlationId
            );

            OrderResponse response = mapper.toResponse(saved);
            response.setEventStatus(eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");

            log.info("Order created successfully: orderId={} | correlationId={} | eventStatus={}",
                    saved.getOrderId(), correlationId, response.getEventStatus());

            return response;

        } finally {
            MDC.remove("correlationId");
        }
    }

    public OrderResponse fallbackCreateOrder(OrderRequest request, Throwable t) {
        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;

        log.error("Fallback triggered for createOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
        return response;
    }

    // ================= Get Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackGetOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrder(UUID id) {
        log.info("Fetching order from DB with id={}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFound("Order not found"));

        OrderResponse response = mapper.toResponse(order);
        response.setEventStatus("UNKNOWN");
        return response;
    }

    public OrderResponse fallbackGetOrder(UUID id, Throwable t) {
        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;
        if (t instanceof OrderNotFound) throw (OrderNotFound) t;

        log.error("Fallback triggered for getOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderId(id);
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
        return response;
    }

    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackgetgetAllOrdersByUserId")
    @RateLimiter(name = "orderServiceRateLimiter")
    @Cacheable(value = "orderByID", key = "#id")
    public List<OrderResponse> getAllOrdersByUserId(UUID id) {

        log.info("Fetching orders for customerId={}", id);

        List<Order> orders = repository.findByCustomerId(id);

        return orders.stream()
                .map(order -> {
                    OrderResponse response = mapper.toResponse(order);
                    response.setEventStatus("UNKNOWN");
                    return response;
                })
                .toList();
    }

    public List<OrderResponse> fallbackgetgetAllOrdersByUserId(UUID id, Throwable t) {

        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;

        log.error("Fallback triggered for getAllOrdersByUserId due to: {}", t.getMessage());

        return Collections.emptyList();
    }

    // ================= Get All Orders =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackGetAllOrders")
    @RateLimiter(name = "orderServiceRateLimiter")
    @Cacheable(value = "allOrders")
    public List<OrderResponse> getAllOrders() {
        log.info("Fetching all orders from DB");
        return repository.findAll().stream()
                .map(order -> {
                    OrderResponse res = mapper.toResponse(order);
                    res.setEventStatus("UNKNOWN");
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<OrderResponse> fallbackGetAllOrders(Throwable t) {
        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;

        log.error("Fallback triggered for getAllOrders due to: {}", t.getMessage());
        return List.of();
    }

    // ================= Update Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackUpdateOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "allOrders", allEntries = true)
    })
    @Transactional
    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("Updating order with id={}", id);
            Order order = repository.findById(id)
                    .orElseThrow(() -> new OrderNotFound("Order with id " + id + " not found for Update"));

            if (request.getCustomerId() != null) order.setCustomerId(parseUUID(request.getCustomerId()));
            if (request.getProductId() != null) order.setProductId(parseUUID(request.getProductId()));

            order.setQuantity(request.getQuantity());
            order.setPrice(request.getPrice());
            order.setUpdatedAt(LocalDateTime.now());
            order.setCorrelationId(correlationId);  // === NEW ===

            repository.save(order);

            boolean eventPublished = sendKafkaEvent(
                    () -> producer.publishOrderUpdated(order, correlationId),
                    id,
                    correlationId
            );

            OrderResponse response = mapper.toResponse(order);
            response.setEventStatus(eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");

            log.info("Order updated successfully: orderId={} | correlationId={} | eventStatus={}",
                    id, correlationId, response.getEventStatus());

            return response;

        } finally {
            MDC.remove("correlationId");
        }
    }

    public OrderResponse fallbackUpdateOrder(UUID id, OrderRequest request, Throwable t) {
        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;
        if (t instanceof OrderNotFound) throw (OrderNotFound) t;

        log.error("Fallback triggered for updateOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderId(id);
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
        return response;
    }

    // ================= Delete Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackDeleteOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "allOrders", allEntries = true)
    })
    @Transactional
    public String deleteOrder(UUID id) throws OrderNotFound {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        try {
            log.info("Deleting order with id={}", id);
            if (!repository.existsById(id)) {
                throw new OrderNotFound("Order with id " + id + " not found for deletion");
            }
            repository.deleteById(id);

            boolean eventPublished = sendKafkaEvent(
                    () -> producer.publishOrderCancelled(id, correlationId),
                    id,
                    correlationId
            );

            log.info("Order deleted successfully: orderId={} | correlationId={} | eventStatus={}",
                    id, correlationId, eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");

            return "Order deleted successfully";

        } finally {
            MDC.remove("correlationId");
        }
    }

    public void fallbackDeleteOrder(UUID id, Throwable t) {
        if (t instanceof RequestNotPermitted) throw (RequestNotPermitted) t;
        if (t instanceof CallNotPermittedException) throw (CallNotPermittedException) t;
        log.error("Fallback triggered for deleteOrder due to: {}", t.getMessage());
    }

    // ================= Helper Methods =================
    private UUID parseUUID(String idStr) {
        try {
            return UUID.fromString(idStr);
        } catch (Exception e) {
            throw new RuntimeException("Invalid UUID: " + idStr);
        }
    }

    // === NEW: Enhanced error handling with correlation ID ===
    private boolean sendKafkaEvent(KafkaCall call, UUID key, String correlationId) {
        try {
            return call.send();
        } catch (Exception e) {
            log.error("[{}] Kafka event failed for key={}: {}",
                    correlationId, key, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface KafkaCall {
        boolean send() throws Exception;
    }
}