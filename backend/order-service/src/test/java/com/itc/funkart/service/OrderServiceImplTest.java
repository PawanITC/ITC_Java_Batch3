package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.exception.OrderNotFound;
import com.itc.funkart.kafka.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private OrderEventProducer producer;

    @InjectMocks
    private OrderServiceImpl service;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    void createOrder_shouldPersistAndPublish_andSetEventStatusPublished() {
        String customerId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        OrderRequest request = new OrderRequest(customerId, productId, 3, 29.99);

        Order mapped = new Order();
        when(mapper.toEntity(request)).thenReturn(mapped);

        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order toSave = invocation.getArgument(0);
            if (toSave.getOrderId() == null) {
                toSave.setOrderId(UUID.randomUUID());
            }
            return toSave;
        });

        when(producer.publishOrderCreated(any(Order.class), anyString())).thenReturn(true);

        when(mapper.toResponse(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            return OrderResponse.builder()
                    .orderId(saved.getOrderId())
                    .customerId(saved.getCustomerId())
                    .productId(saved.getProductId())
                    .quantity(saved.getQuantity())
                    .price(saved.getPrice())
                    .orderStatus(saved.getOrderStatus())
                    .build();
        });

        OrderResponse response = service.createOrder(request);

        assertNotNull(response.getOrderId());
        assertEquals(UUID.fromString(customerId), response.getCustomerId());
        assertEquals(UUID.fromString(productId), response.getProductId());
        assertEquals("CREATED", response.getOrderStatus());
        assertEquals("PUBLISHED", response.getEventStatus());

        verify(repository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getCorrelationId());
        verify(producer).publishOrderCreated(eq(saved), anyString());
    }

    @Test
    void createOrder_shouldReturnNotPublishedWhenProducerReturnsFalse() {
        OrderRequest request = new OrderRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                1,
                2.0
        );

        when(mapper.toEntity(request)).thenReturn(new Order());
        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setOrderId(UUID.randomUUID());
            return saved;
        });
        when(producer.publishOrderCreated(any(Order.class), anyString())).thenReturn(false);
        when(mapper.toResponse(any(Order.class))).thenReturn(OrderResponse.builder().orderStatus("CREATED").build());

        OrderResponse response = service.createOrder(request);

        assertEquals("NOT_PUBLISHED", response.getEventStatus());
    }

    @Test
    void createOrder_shouldThrowOnInvalidUuid() {
        OrderRequest request = new OrderRequest("not-a-uuid", UUID.randomUUID().toString(), 1, 1.0);
        assertThrows(RuntimeException.class, () -> service.createOrder(request));
    }

    @Test
    void createOrder_shouldSurfaceKafkaFailures() {
        OrderRequest request = new OrderRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                1,
                2.0
        );
        when(mapper.toEntity(request)).thenReturn(new Order());
        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setOrderId(UUID.randomUUID());
            return saved;
        });
        when(producer.publishOrderCreated(any(Order.class), anyString()))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> service.createOrder(request));
    }

    @Test
    void getOrder_shouldReturnMapped_andSetEventStatusUnknown() {
        UUID orderId = UUID.randomUUID();
        Order stored = Order.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(15.0)
                .orderStatus("CREATED")
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(stored));
        when(mapper.toResponse(stored)).thenReturn(OrderResponse.builder()
                .orderId(orderId)
                .customerId(stored.getCustomerId())
                .productId(stored.getProductId())
                .quantity(stored.getQuantity())
                .price(stored.getPrice())
                .orderStatus(stored.getOrderStatus())
                .build());

        OrderResponse response = service.getOrder(orderId);

        assertEquals(orderId, response.getOrderId());
        assertEquals("UNKNOWN", response.getEventStatus());
    }

    @Test
    void getOrder_shouldThrowWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFound.class, () -> service.getOrder(orderId));
    }

    @Test
    void fallbackGetOrder_shouldReturnServiceUnavailableResponse() {
        UUID orderId = UUID.randomUUID();

        OrderResponse response = service.fallbackGetOrder(orderId, new RuntimeException("db down"));

        assertEquals(orderId, response.getOrderId());
        assertEquals("SERVICE_UNAVAILABLE", response.getOrderStatus());
        assertEquals("NOT_PUBLISHED", response.getEventStatus());
    }

    @Test
    void fallbackGetOrder_shouldRethrowOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        assertThrows(OrderNotFound.class, () -> service.fallbackGetOrder(orderId, new OrderNotFound("missing")));
    }

    @Test
    void getAllOrders_shouldMapAll_andSetEventStatusUnknown() {
        Order first = Order.builder().orderId(UUID.randomUUID()).build();
        Order second = Order.builder().orderId(UUID.randomUUID()).build();
        when(repository.findAll()).thenReturn(List.of(first, second));
        when(mapper.toResponse(any(Order.class))).thenAnswer(invocation -> {
            Order src = invocation.getArgument(0);
            return OrderResponse.builder().orderId(src.getOrderId()).build();
        });

        List<OrderResponse> out = service.getAllOrders();

        assertEquals(2, out.size());
        assertEquals("UNKNOWN", out.get(0).getEventStatus());
        assertEquals("UNKNOWN", out.get(1).getEventStatus());
    }

    @Test
    void fallbackGetAllOrders_shouldReturnEmptyList() {
        assertEquals(0, service.fallbackGetAllOrders(new RuntimeException("boom")).size());
    }

    @Test
    void getAllOrdersByUserId_shouldQueryRepository_andSetEventStatusUnknown() {
        UUID userId = UUID.randomUUID();
        Order stored = Order.builder().orderId(UUID.randomUUID()).customerId(userId).build();
        when(repository.findByCustomerId(userId)).thenReturn(List.of(stored));
        when(mapper.toResponse(stored)).thenReturn(OrderResponse.builder().orderId(stored.getOrderId()).build());

        List<OrderResponse> out = service.getAllOrdersByUserId(userId);

        assertEquals(1, out.size());
        assertEquals("UNKNOWN", out.get(0).getEventStatus());
        verify(repository).findByCustomerId(userId);
    }

    @Test
    void fallbackgetgetAllOrdersByUserId_shouldReturnEmptyList() {
        assertEquals(0, service.fallbackgetgetAllOrdersByUserId(UUID.randomUUID(), new RuntimeException("boom")).size());
    }

    @Test
    void updateOrder_shouldPersistAndPublish_andSetEventStatusPublished() {
        UUID orderId = UUID.randomUUID();
        Order existing = Order.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(1)
                .price(5.0)
                .orderStatus("CREATED")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(producer.publishOrderUpdated(eq(existing), anyString())).thenReturn(true);
        when(mapper.toResponse(existing)).thenAnswer(invocation -> {
            Order src = invocation.getArgument(0);
            return OrderResponse.builder()
                    .orderId(src.getOrderId())
                    .customerId(src.getCustomerId())
                    .productId(src.getProductId())
                    .quantity(src.getQuantity())
                    .price(src.getPrice())
                    .orderStatus(src.getOrderStatus())
                    .build();
        });

        String updatedCustomerId = UUID.randomUUID().toString();
        String updatedProductId = UUID.randomUUID().toString();
        OrderRequest request = new OrderRequest(updatedCustomerId, updatedProductId, 5, 35.0);

        OrderResponse response = service.updateOrder(orderId, request);

        assertEquals(orderId, response.getOrderId());
        assertEquals(UUID.fromString(updatedCustomerId), existing.getCustomerId());
        assertEquals(UUID.fromString(updatedProductId), existing.getProductId());
        assertNotNull(existing.getUpdatedAt());
        assertNotNull(existing.getCorrelationId());
        assertEquals("PUBLISHED", response.getEventStatus());
    }

    @Test
    void updateOrder_shouldReturnNotPublishedWhenProducerReturnsFalse() {
        UUID orderId = UUID.randomUUID();
        Order existing = Order.builder().orderId(orderId).build();
        when(repository.findById(orderId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(producer.publishOrderUpdated(eq(existing), anyString())).thenReturn(false);
        when(mapper.toResponse(existing)).thenReturn(OrderResponse.builder().orderId(orderId).build());

        OrderRequest request = new OrderRequest(null, null, 1, 1.0);
        OrderResponse response = service.updateOrder(orderId, request);

        assertEquals("NOT_PUBLISHED", response.getEventStatus());
    }

    @Test
    void fallbackUpdateOrder_shouldReturnServiceUnavailableResponse() {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = service.fallbackUpdateOrder(
                orderId,
                new OrderRequest(null, null, 1, 1.0),
                new RuntimeException("boom")
        );
        assertEquals(orderId, response.getOrderId());
        assertEquals("SERVICE_UNAVAILABLE", response.getOrderStatus());
        assertEquals("NOT_PUBLISHED", response.getEventStatus());
    }

    @Test
    void updateOrder_shouldThrowWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFound.class, () -> service.updateOrder(orderId, new OrderRequest(null, null, 1, 1.0)));
    }

    @Test
    void fallbackDeleteOrder_shouldNotThrowOnGenericErrors() {
        service.fallbackDeleteOrder(UUID.randomUUID(), new RuntimeException("boom"));
    }

    @Test
    void deleteOrder_shouldDeleteAndPublish_andReturnMessage() {
        UUID orderId = UUID.randomUUID();
        when(repository.existsById(orderId)).thenReturn(true);
        when(producer.publishOrderCancelled(eq(orderId), anyString())).thenReturn(true);

        String result = service.deleteOrder(orderId);

        assertEquals("Order deleted successfully", result);
        verify(repository).deleteById(orderId);
        verify(producer).publishOrderCancelled(eq(orderId), anyString());
    }

    @Test
    void deleteOrder_shouldThrowWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.existsById(orderId)).thenReturn(false);

        assertThrows(OrderNotFound.class, () -> service.deleteOrder(orderId));
    }

    @Test
    void fallbackCreateOrder_shouldRethrowRateLimiter() {
        assertThrows(RequestNotPermitted.class, () -> service.fallbackCreateOrder(
                new OrderRequest(null, null, null, null),
                mock(RequestNotPermitted.class)
        ));
    }

    @Test
    void fallbackCreateOrder_shouldRethrowCircuitBreaker() {
        CallNotPermittedException cbOpen = mock(CallNotPermittedException.class);
        assertThrows(CallNotPermittedException.class, () -> service.fallbackCreateOrder(
                new OrderRequest(null, null, null, null),
                cbOpen
        ));
    }
}
