package com.itc.funkart.aggregator.service.impl;

import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.common.enums.order.OrderStatus;
import com.itc.funkart.aggregator.dto.OrderResponse;
import com.itc.funkart.aggregator.entity.Order;
import com.itc.funkart.aggregator.exception.OrderBadRequestException;
import com.itc.funkart.aggregator.exception.OrderNotFoundException;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.aggregator.repository.OrderRepository;
import com.itc.funkart.aggregator.service.KafkaEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <h2>OrderServiceImplTest</h2>
 *
 * <p>Fixed from original commented-out version:</p>
 * <ul>
 *   <li>Corrected {@code CheckoutInitiatedEvent} to 6-component canonical constructor.</li>
 *   <li>Updated {@code repository.save} → {@code saveAndFlush} throughout.</li>
 *   <li>Updated initiation event from {@code sendOrderEvent} → {@code sendOrderCreated}.</li>
 *   <li>Added tests for {@code cancelOrder}, {@code getOrderByIdAndUserId},
 *       {@code getCustomerOrderHistory}, and pagination.</li>
 * </ul>
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

    // =========================================================
    // processOrderInitiation
    // =========================================================

    @Nested
    @DisplayName("processOrderInitiation")
    class OrderInitiation {

        @Test
        @DisplayName("Success: persists order and invokes sendOrderCreated")
        void success() {
            CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
                    OrderEventType.ORDER_INITIATED, 100L, new BigDecimal("500.00"),
                    List.of(), "usd", null, null);

            Order entity = new Order();
            entity.setCustomerId(100L);
            Order saved = new Order();
            saved.setId(2001L);
            saved.setCustomerId(100L);

            when(mapper.toEntity(event)).thenReturn(entity);
            when(repository.saveAndFlush(any(Order.class))).thenReturn(saved);

            service.processOrderInitiation(event);

            verify(repository).saveAndFlush(entity);
            verify(eventService).sendOrderCreated(saved);
            verifyNoMoreInteractions(eventService);
        }
    }

    // =========================================================
    // cancelOrder
    // =========================================================

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("PENDING -> CANCELLED and fires ORDER_CANCELLED")
        void pendingSuccess() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.PENDING);
            when(repository.findByIdAndCustomerIdWithItems(1L, 100L)).thenReturn(Optional.of(order));
            when(repository.saveAndFlush(any())).thenReturn(order);

            service.cancelOrder(1L, 100L);

            verify(repository).saveAndFlush(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
            verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_CANCELLED));
        }

        @Test
        @DisplayName("PAID -> CANCELLED (still cancellable)")
        void paidSuccess() {
            Order order = new Order();
            order.setId(2L);
            order.setStatus(OrderStatus.PAID);
            when(repository.findByIdAndCustomerIdWithItems(2L, 42L)).thenReturn(Optional.of(order));
            when(repository.saveAndFlush(any())).thenReturn(order);

            service.cancelOrder(2L, 42L);

            verify(repository).saveAndFlush(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
            verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_CANCELLED));
        }

        @Test
        @DisplayName("Owner mismatch: throws OrderNotFoundException")
        void notFound() {
            when(repository.findByIdAndCustomerIdWithItems(99L, 1L)).thenReturn(Optional.empty());
            assertThrows(OrderNotFoundException.class, () -> service.cancelOrder(99L, 1L));
            verify(repository, never()).saveAndFlush(any());
            verifyNoInteractions(eventService);
        }

        @ParameterizedTest(name = "Status {0} is non-cancellable")
        @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "DELIVERED", "CANCELLED", "FAILED", "REFUNDED"})
        @DisplayName("Non-cancellable states throw OrderBadRequestException")
        void nonCancellableStates(OrderStatus status) {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(status);
            when(repository.findByIdAndCustomerIdWithItems(1L, 100L)).thenReturn(Optional.of(order));

            assertThrows(OrderBadRequestException.class, () -> service.cancelOrder(1L, 100L));
            verify(repository, never()).saveAndFlush(any());
            verifyNoInteractions(eventService);
        }
    }

    // =========================================================
    // updateOrderStatus
    // =========================================================

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateStatus {

        @Test
        @DisplayName("SHIPPED: saves and broadcasts ORDER_SHIPPED")
        void shipped() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.PAID);
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(repository.saveAndFlush(any())).thenReturn(order);
            when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().orderId(1L).build());

            service.updateOrderStatus(1L, OrderStatus.SHIPPED);

            verify(repository).saveAndFlush(argThat(o -> o.getStatus() == OrderStatus.SHIPPED));
            verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_SHIPPED));
        }

        @Test
        @DisplayName("PAID: broadcasts PAYMENT_SUCCESS")
        void paid() {
            Order order = new Order();
            order.setId(3L);
            order.setStatus(OrderStatus.PENDING);
            when(repository.findById(3L)).thenReturn(Optional.of(order));
            when(repository.saveAndFlush(any())).thenReturn(order);
            when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().orderId(3L).build());

            service.updateOrderStatus(3L, OrderStatus.PAID);

            verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.PAYMENT_SUCCESS));
        }

        @Test
        @DisplayName("CANCELLED: broadcasts ORDER_CANCELLED")
        void cancelled() {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(OrderStatus.PENDING);
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(repository.saveAndFlush(any())).thenReturn(order);
            when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().build());

            service.updateOrderStatus(1L, OrderStatus.CANCELLED);

            verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_CANCELLED));
        }

        @ParameterizedTest(name = "Terminal {0} blocks update")
        @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED", "FAILED", "REFUNDED", "CONFIRMED"})
        @DisplayName("All final states throw OrderBadRequestException")
        void allFinalStatesBlocked(OrderStatus finalStatus) {
            Order order = new Order();
            order.setId(1L);
            order.setStatus(finalStatus);
            when(repository.findById(1L)).thenReturn(Optional.of(order));

            assertThrows(OrderBadRequestException.class,
                    () -> service.updateOrderStatus(1L, OrderStatus.SHIPPED));
            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Unknown ID: throws OrderNotFoundException")
        void notFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(OrderNotFoundException.class,
                    () -> service.updateOrderStatus(999L, OrderStatus.SHIPPED));
        }
    }

    // =========================================================
    // Queries
    // =========================================================

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("getOrder: success")
        void getOrder_success() {
            Order order = new Order();
            OrderResponse expected = OrderResponse.builder().orderId(1L).build();
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(mapper.toResponse(order)).thenReturn(expected);
            assertEquals(expected, service.getOrder(1L));
        }

        @Test
        @DisplayName("getOrder: unknown ID throws NotFound")
        void getOrder_notFound() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(OrderNotFoundException.class, () -> service.getOrder(999L));
        }

        @Test
        @DisplayName("getOrderByIdAndUserId: success")
        void getOrderByIdAndUserId_success() {
            Order order = new Order();
            OrderResponse expected = OrderResponse.builder().orderId(1L).customerId(42L).build();
            when(repository.findByIdAndCustomerIdWithItems(1L, 42L)).thenReturn(Optional.of(order));
            when(mapper.toResponse(order)).thenReturn(expected);
            assertEquals(expected, service.getOrderByIdAndUserId(1L, 42L));
        }

        @Test
        @DisplayName("getOrderByIdAndUserId: owner mismatch throws NotFound")
        void getOrderByIdAndUserId_notFound() {
            when(repository.findByIdAndCustomerIdWithItems(1L, 99L)).thenReturn(Optional.empty());
            assertThrows(OrderNotFoundException.class,
                    () -> service.getOrderByIdAndUserId(1L, 99L));
        }

        @Test
        @DisplayName("getCustomerOrderHistory: returns mapped list")
        void getCustomerOrderHistory_success() {
            Order order = new Order();
            OrderResponse dto = OrderResponse.builder().orderId(5L).build();
            when(repository.findByCustomerIdWithItems(42L)).thenReturn(List.of(order));
            when(mapper.toResponse(order)).thenReturn(dto);

            List<OrderResponse> result = service.getCustomerOrderHistory(42L);
            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }

        @Test
        @DisplayName("getCustomerOrderHistory: empty list when no orders")
        void getCustomerOrderHistory_empty() {
            when(repository.findByCustomerIdWithItems(99L)).thenReturn(List.of());
            assertTrue(service.getCustomerOrderHistory(99L).isEmpty());
            verify(mapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("getAllOrders: returns paginated responses")
        void getAllOrders_success() {
            Pageable pageable = PageRequest.of(0, 10);
            Order order = new Order();
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order)));
            when(mapper.toResponse(order)).thenReturn(OrderResponse.builder().orderId(1L).build());

            Page<OrderResponse> result = service.getAllOrders(pageable);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("getAllOrders: returns empty page")
        void getAllOrders_empty() {
            Pageable pageable = PageRequest.of(0, 10);
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));
            assertTrue(service.getAllOrders(pageable).getContent().isEmpty());
            verify(mapper, never()).toResponse(any());
        }
    }
}
