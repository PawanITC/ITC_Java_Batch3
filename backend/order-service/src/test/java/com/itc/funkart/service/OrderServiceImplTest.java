package com.itc.funkart.service;

import com.itc.funkart.dto.OrderRequest;
import com.itc.funkart.dto.OrderResponse;
import com.itc.funkart.entity.Order;
import com.itc.funkart.exception.OrderNotFound;
import com.itc.funkart.kafka.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void createOrder_shouldPersistOrderAndPublishEvent() throws Exception {
        OrderRequest request = new OrderRequest(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                3,
                29.99
        );

        Order transientOrder = new Order();
        when(mapper.toEntity(request)).thenReturn(transientOrder);

        AtomicReference<Order> savedOrderRef = new AtomicReference<>();
        when(repository.save(any(Order.class))).thenAnswer(invocation -> {
            Order toSave = invocation.getArgument(0);
            Order saved = Order.builder()
                    .orderId(UUID.randomUUID())
                    .customerId(toSave.getCustomerId())
                    .productId(toSave.getProductId())
                    .quantity(toSave.getQuantity())
                    .price(toSave.getPrice())
                    .orderStatus(toSave.getOrderStatus())
                    .createdAt(toSave.getCreatedAt())
                    .updatedAt(toSave.getUpdatedAt())
                    .build();
            savedOrderRef.set(saved);
            return saved;
        });

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

        Order savedOrder = savedOrderRef.get();
        assertNotNull(savedOrder);
        assertEquals("CREATED", savedOrder.getOrderStatus());
        assertEquals(savedOrder.getOrderId(), response.getOrderId());
        assertEquals(savedOrder.getCustomerId(), response.getCustomerId());
        assertEquals(savedOrder.getProductId(), response.getProductId());
        assertEquals(savedOrder.getQuantity(), response.getQuantity());
        assertEquals(savedOrder.getPrice(), response.getPrice());

        verify(repository).save(transientOrder);
        verify(producer).publishOrderCreated(savedOrder);
    }

    @Test
    void getOrder_shouldReturnMappedResponse() {
        UUID orderId = UUID.randomUUID();
        Order storedOrder = Order.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(2)
                .price(15.0)
                .orderStatus("CREATED")
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(storedOrder));
        when(mapper.toResponse(storedOrder)).thenReturn(OrderResponse.builder()
                .orderId(orderId)
                .customerId(storedOrder.getCustomerId())
                .productId(storedOrder.getProductId())
                .quantity(storedOrder.getQuantity())
                .price(storedOrder.getPrice())
                .orderStatus(storedOrder.getOrderStatus())
                .build());

        OrderResponse response = service.getOrder(orderId);

        assertEquals(orderId, response.getOrderId());
        assertEquals(storedOrder.getCustomerId(), response.getCustomerId());
        assertEquals(storedOrder.getProductId(), response.getProductId());
        assertEquals(storedOrder.getQuantity(), response.getQuantity());
        assertEquals(storedOrder.getPrice(), response.getPrice());
    }

    @Test
    void getOrder_shouldThrowWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getOrder(orderId));
    }

    @Test
    void getAllOrders_shouldReturnAllMapped() {
        List<Order> stored = List.of(
                Order.builder()
                        .orderId(UUID.randomUUID())
                        .customerId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(1)
                        .price(10.0)
                        .orderStatus("CREATED")
                        .build(),
                Order.builder()
                        .orderId(UUID.randomUUID())
                        .customerId(UUID.randomUUID())
                        .productId(UUID.randomUUID())
                        .quantity(4)
                        .price(40.0)
                        .orderStatus("CREATED")
                        .build()
        );

        when(repository.findAll()).thenReturn(stored);

        List<OrderResponse> responses = service.getAllOrders();

        assertEquals(2, responses.size());
        assertEquals(stored.get(0).getOrderId(), responses.get(0).getOrderId());
        assertEquals(stored.get(1).getProductId(), responses.get(1).getProductId());
    }

    @Test
    void updateOrder_shouldPersistChanges() throws Exception {
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
        String updatedCustomerIdString = UUID.randomUUID().toString();
        String updatedProductIdString = UUID.randomUUID().toString();
        OrderRequest update = new OrderRequest(
                updatedCustomerIdString,
                updatedProductIdString,
                5,
                35.0
        );
        UUID updatedCustomerId = UUID.fromString(updatedCustomerIdString);
        UUID updatedProductId = UUID.fromString(updatedProductIdString);

        when(mapper.toResponse(existing)).thenAnswer(invocation -> {
            Order updatedOrder = invocation.getArgument(0);
            return OrderResponse.builder()
                    .orderId(updatedOrder.getOrderId())
                    .customerId(updatedOrder.getCustomerId())
                    .productId(updatedOrder.getProductId())
                    .quantity(updatedOrder.getQuantity())
                    .price(updatedOrder.getPrice())
                    .orderStatus(updatedOrder.getOrderStatus())
                    .build();
        });

        OrderResponse response = service.updateOrder(orderId, update);

        assertEquals(5, response.getQuantity());
        assertEquals(35.0, response.getPrice());
        assertNotNull(existing.getUpdatedAt());
        assertEquals(orderId, response.getOrderId());
        assertEquals(updatedCustomerId, existing.getCustomerId());
        assertEquals(updatedProductId, existing.getProductId());

        verify(repository).save(existing);
        verify(producer).publishOrderUpdated(existing);
    }

    @Test
    void updateOrder_shouldThrowWhenMissing() {
        UUID orderId = UUID.randomUUID();
        when(repository.findById(orderId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.updateOrder(orderId, new OrderRequest(null, null, 1, 1.0)));
    }

    @Test
    void deleteOrder_shouldRelayToRepositoryAndEventStream() throws Exception {
        UUID orderId = UUID.randomUUID();

        service.deleteOrder(orderId);

        verify(repository).deleteById(orderId);
        verify(producer).publishOrderCancelled(orderId);
    }
}
