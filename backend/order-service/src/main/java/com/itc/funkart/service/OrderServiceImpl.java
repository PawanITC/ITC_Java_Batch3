
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
import org.aspectj.weaver.ast.Call;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for customerId={} and productId={}", request.getCustomerId(), request.getProductId());

        UUID customerId = parseUUID(request.getCustomerId());
        UUID productId = parseUUID(request.getProductId());

        Order order = mapper.toEntity(request);
        order.setCustomerId(customerId);
        order.setProductId(productId);
        order.setOrderStatus("CREATED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        Order saved = repository.save(order);

        boolean eventPublished = sendKafkaEvent(() -> producer.publishOrderCreated(saved), saved.getOrderId());

        OrderResponse response = mapper.toResponse(saved);
        response.setEventStatus(eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");
//        response.setMessage(eventPublished ? "Order created successfully" : "Order created but event failed");

        log.info("Order created successfully: orderId={} | eventStatus={}", saved.getOrderId(), response.getEventStatus());
        return response;
    }

    public OrderResponse fallbackCreateOrder(OrderRequest request, Throwable t) {
        if(t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        if(t instanceof CallNotPermittedException) {
            throw (CallNotPermittedException) t;

        }

        log.error("Fallback triggered for createOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
//        response.setMessage("Service temporarily unavailable, please try again later");
        return response;
    }

    // ================= Get Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackGetOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    public OrderResponse getOrder(UUID id) {
        log.info("Fetching order with id={}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFound("Order not found"));

        OrderResponse response = mapper.toResponse(order);
        response.setEventStatus("UNKNOWN");
        return response;
    }

    public OrderResponse fallbackGetOrder(UUID id, Throwable t) {
        if (t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t; // let it go to GlobalExceptionHandler
        }
        if(t instanceof CallNotPermittedException) {
            throw (CallNotPermittedException) t;
        }
        if (t instanceof OrderNotFound) throw (OrderNotFound) t;
        log.error("Fallback triggered for getOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderId(id);
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
//        response.setMessage("Service temporarily unavailable, please try again later");
        return response;
    }

    // ================= Get All Orders =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackGetAllOrders")
    @RateLimiter(name = "orderServiceRateLimiter")
    public List<OrderResponse> getAllOrders() {
        return repository.findAll().stream()
                .map(order -> {
                    OrderResponse res = mapper.toResponse(order);
                    res.setEventStatus("UNKNOWN");
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<OrderResponse> fallbackGetAllOrders(Throwable t) {
        if(t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        if(t instanceof CallNotPermittedException)
        {
            throw (CallNotPermittedException) t;
        }

        log.error("Fallback triggered for getAllOrders due to: {}", t.getMessage());
        return List.of();
    }

    // ================= Update Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackUpdateOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    public OrderResponse updateOrder(UUID id, OrderRequest request) {
        log.info("Updating order with id={}", id);
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderNotFound("Order with id " + id + " not found for Update"));

        if (request.getCustomerId() != null) order.setCustomerId(parseUUID(request.getCustomerId()));
        if (request.getProductId() != null) order.setProductId(parseUUID(request.getProductId()));

        order.setQuantity(request.getQuantity());
        order.setPrice(request.getPrice());
        order.setUpdatedAt(LocalDateTime.now());


        repository.save(order);

        boolean eventPublished = sendKafkaEvent(() -> producer.publishOrderUpdated(order), id);

        OrderResponse response = mapper.toResponse(order);
        response.setEventStatus(eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");
//        response.setMessage(eventPublished ? "Order updated successfully" : "Order updated but event failed");

        log.info("Order updated successfully: orderId={} | eventStatus={}", id, response.getEventStatus());
        return response;
    }

    public OrderResponse fallbackUpdateOrder(UUID id, OrderRequest request, Throwable t) {
        if(t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        if(t instanceof CallNotPermittedException)
        {
            throw (CallNotPermittedException) t;
        }
        if (t instanceof OrderNotFound) throw (OrderNotFound) t;
        log.error("Fallback triggered for updateOrder due to: {}", t.getMessage());
        OrderResponse response = new OrderResponse();
        response.setOrderId(id);
        response.setOrderStatus("SERVICE_UNAVAILABLE");
        response.setEventStatus("NOT_PUBLISHED");
//        response.setMessage("Service temporarily unavailable, please try again later");
        return response;
    }

    // ================= Delete Order =================
    @Override
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackDeleteOrder")
    @RateLimiter(name = "orderServiceRateLimiter")
    public String deleteOrder(UUID id)  {
        log.info("Deleting order with id={}", id);
        if (!repository.existsById(id))
        {
//            log.warn("Order with id={} not found for deletion", id);
            throw new OrderNotFound("Order with id " + id + " not found for deletion");
//            return;
        }
        repository.deleteById(id);

        boolean eventPublished = sendKafkaEvent(() -> producer.publishOrderCancelled(id), id);
        log.info("Order deleted successfully: orderId={} | eventStatus={}", id, eventPublished ? "PUBLISHED" : "NOT_PUBLISHED");
        return "Order deleted successfully";
    }


    public void fallbackDeleteOrder(UUID id, Throwable t) {
        if(t instanceof RequestNotPermitted) {
            throw (RequestNotPermitted) t;
        }
        if (t instanceof CallNotPermittedException) {
            throw (CallNotPermittedException) t; // let handler return 503
        }
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

    private boolean sendKafkaEvent(KafkaCall call, UUID key) {
        try {
            return call.send();
        } catch (Exception e) {
            log.error("Kafka event failed for key={}: {}", key, e.getMessage(), e);
            throw new RuntimeException(e); // propagate to circuit breaker/fallback
        }
    }

    @FunctionalInterface
    interface KafkaCall {
        boolean send() throws Exception;
    }
}


