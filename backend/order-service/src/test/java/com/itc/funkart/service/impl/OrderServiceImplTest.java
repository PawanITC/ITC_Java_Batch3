package com.itc.funkart.service.impl;

import com.itc.funkart.dto.OrderItemRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl covering:
 * - happy paths
 * - validation branches
 * - exception branches
 * - circuit breaker fallback
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private KafkaEventService eventService;

    @InjectMocks
    private OrderServiceImpl service;


    // ---------------- CREATE ORDER ----------------

    @Test
    @DisplayName("createOrder - success path publishes event")
    void createOrder_success() {

        // ----- REQUEST -----
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));

        // ----- ENTITY (after mapper) -----
        OrderItem item = new OrderItem();
        item.setProductId(10L);
        item.setQuantity(2);
        item.setPriceAtPurchase(BigDecimal.valueOf(100));

        Order entity = new Order();
        entity.setItems(List.of(item));

        // ----- SAVED ENTITY -----
        Order saved = new Order();
        saved.setId(1L);
        saved.setStatus(OrderStatus.PENDING);
        saved.setCustomerId(10L);
        saved.setItems(List.of(item));

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any(Order.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(
                OrderResponse.builder()
                        .orderId(1L)
                        .build()
        );

        when(eventService.sendOrderEvent(saved)).thenReturn(true);

        // ACT
        OrderResponse response = service.createOrder(request, 10L);

        // ASSERT
        assertEquals(1L, response.getOrderId());
        assertNotNull(response);
        assertEquals("PUBLISHED", response.getEventStatus());

        verify(repository).save(any(Order.class));
        verify(eventService).sendOrderEvent(any(Order.class));
    }

    @Test
    @DisplayName("createOrder - empty items throws exception")
    void createOrder_emptyItems() {

        OrderRequest request = new OrderRequest();
        request.setItems(List.of());

        assertThrows(OrderBadRequestException.class,
                () -> service.createOrder(request, 1L));
    }

    @Test
    @DisplayName("createOrder - event failure branch sets PENDING_RETRY")
    void createOrder_eventFailure() {

        // ----- REQUEST -----
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));

        // ----- ENTITY -----
        OrderItem item = new OrderItem();
        item.setProductId(10L);
        item.setQuantity(1);
        item.setPriceAtPurchase(BigDecimal.valueOf(100));

        Order entity = new Order();
        entity.setItems(List.of(item));

        // ----- SAVED -----
        Order saved = new Order();
        saved.setId(1L);
        saved.setStatus(OrderStatus.PENDING);
        saved.setCustomerId(1L);
        saved.setItems(List.of(item));

        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(any(Order.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(
                OrderResponse.builder()
                        .orderId(1L)
                        .build()
        );

        when(eventService.sendOrderEvent(saved)).thenReturn(false);

        // ACT
        OrderResponse response = service.createOrder(request, 1L);

        // ASSERT
        assertEquals("PENDING_RETRY", response.getEventStatus());

        verify(repository).save(any(Order.class));
        verify(eventService).sendOrderEvent(saved);
    }

    // ---------------- GET ORDER ----------------

    @Test
    void getOrder_notFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> service.getOrder(1L));
    }

    // ---------------- UPDATE STATUS ----------------

    @Test
    void updateStatus_terminalState_blocked() {

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CANCELLED);

        when(repository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(OrderBadRequestException.class,
                () -> service.updateOrderStatus(1L, OrderStatus.SHIPPED));

        verify(repository, never()).save(any());
    }

    @Test
    void updateStatus_success() {

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.PENDING);

        when(repository.findById(1L)).thenReturn(Optional.of(order));
        when(repository.save(any())).thenReturn(order);
        when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        OrderResponse res = service.updateOrderStatus(1L, OrderStatus.SHIPPED);

        assertNotNull(res);
        verify(eventService).sendOrderEvent(order);
    }

    // ---------------- SEARCH BRANCH ----------------

    @Test
    void searchOrders_statusFilterBranch() {

        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByStatus(eq(OrderStatus.PENDING), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(new Order())));

        when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

        Page<OrderResponse> result =
                service.searchOrders(null, OrderStatus.PENDING, null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(repository).findByStatus(OrderStatus.PENDING, pageable);
    }

    // ---------------- FALLBACK ----------------

    @Test
    void fallback_createOrder() {
        OrderResponse res = service.fallbackCreateOrder(
                new OrderRequest(), 1L, new RuntimeException("down"));

        assertEquals("SERVICE_BUSY_RETRY_LATER", res.getEventStatus());
    }
}