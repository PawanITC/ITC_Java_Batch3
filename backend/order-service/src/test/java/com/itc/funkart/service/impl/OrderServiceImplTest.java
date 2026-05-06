//package com.itc.funkart.service.impl;
//
//import com.itc.funkart.common.dto.event.checkout.CheckoutInitiatedEvent;
//import com.itc.funkart.common.enums.order.OrderEventType;
//import com.itc.funkart.common.enums.order.OrderStatus;
//import com.itc.funkart.dto.OrderResponse;
//import com.itc.funkart.entity.Order;
//import com.itc.funkart.exception.OrderBadRequestException;
//import com.itc.funkart.exception.OrderNotFoundException;
//import com.itc.funkart.mapper.OrderMapper;
//import com.itc.funkart.repository.OrderRepository;
//import com.itc.funkart.service.KafkaEventService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.*;
//
///**
// * <h2>OrderServiceImplTest</h2>
// * <p>
// * Comprehensive suite verifying business logic, event broadcasting,
// * and terminal state transitions in the Order domain.
// * </p>
// */
//@ExtendWith(MockitoExtension.class)
//@ActiveProfiles("test")
//class OrderServiceImplTest {
//
//    @Mock
//    private OrderRepository repository;
//
//    @Mock
//    private OrderMapper mapper;
//
//    @Mock
//    private KafkaEventService eventService;
//
//    @InjectMocks
//    private OrderServiceImpl service;
//
//    // ---------------- INITIATION FLOW ----------------
//
//    @Test
//    @DisplayName("processOrderInitiation - success path persists and triggers Kafka")
//    void processOrderInitiation_success() {
//        // Arrange
//        CheckoutInitiatedEvent event = new CheckoutInitiatedEvent(
//                OrderEventType.ORDER_INITIATED, 2001L, 100L, new BigDecimal("500.00"), List.of(), "usd", null
//        );
//
//
//        Order entity = new Order();
//        entity.setCustomerId(100L);
//
//        Order saved = new Order();
//        saved.setId(2001L);
//        saved.setCustomerId(100L);
//
//        when(mapper.toEntity(event)).thenReturn(entity);
//        when(repository.save(any(Order.class))).thenReturn(saved);
//
//        // Act
//        service.processOrderInitiation(event);
//
//        // Assert:
//        // 3. UPDATE VERIFICATION: Verify the service call, not the producer call
//        verify(repository).save(entity);
//        verify(eventService).sendOrderEvent(eq(saved), eq(OrderEventType.ORDER_INITIATED));
//    }
//
//    // ---------------- UPDATE STATUS FLOW ----------------
//
//    @Test
//    @DisplayName("updateOrderStatus - SHIPPED success persists and broadcasts")
//    void updateStatus_success() {
//        // Arrange
//        Order order = new Order();
//        order.setId(1L);
//        order.setStatus(OrderStatus.PENDING);
//
//        when(repository.findById(1L)).thenReturn(Optional.of(order));
//        when(repository.save(any())).thenReturn(order);
//        when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().orderId(1L).build());
//
//        // Act
//        service.updateOrderStatus(1L, OrderStatus.SHIPPED);
//
//        // Assert
//        verify(repository).save(argThat(o -> o.getStatus() == OrderStatus.SHIPPED));
//
//        // UPDATED: Now that ORDER_SHIPPED exists, we verify it!
//        verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_SHIPPED));
//    }
//
//    @Test
//    @DisplayName("updateOrderStatus - CANCELLED triggers specialized record via EventService")
//    void updateStatus_cancellation_success() {
//        // Arrange
//        Order order = new Order();
//        order.setId(1L);
//        order.setStatus(OrderStatus.PENDING);
//
//        when(repository.findById(1L)).thenReturn(Optional.of(order));
//        when(repository.save(any(Order.class))).thenReturn(order);
//        when(mapper.toResponse(any())).thenReturn(OrderResponse.builder().build());
//
//        // Act
//        service.updateOrderStatus(1L, OrderStatus.CANCELLED);
//
//        // Assert
//        verify(repository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
//        verify(eventService).sendOrderEvent(eq(order), eq(OrderEventType.ORDER_CANCELLED));
//    }
//
//    @Test
//    @DisplayName("updateOrderStatus - terminal state (CANCELLED) blocks updates")
//    void updateStatus_terminalState_blocked() {
//        // Arrange
//        Order order = new Order();
//        order.setId(1L);
//        order.setStatus(OrderStatus.CANCELLED);
//
//        when(repository.findById(1L)).thenReturn(Optional.of(order));
//
//        // Act & Assert
//        assertThrows(OrderBadRequestException.class,
//                () -> service.updateOrderStatus(1L, OrderStatus.SHIPPED));
//
//        verify(repository, never()).save(any());
//        verifyNoInteractions(eventService);
//    }
//
//    // ---------------- GET & SEARCH ----------------
//
//    @Test
//    @DisplayName("getOrder - throws NotFound if ID is invalid")
//    void getOrder_notFound() {
//        when(repository.findById(999L)).thenReturn(Optional.empty());
//        assertThrows(OrderNotFoundException.class, () -> service.getOrder(999L));
//    }
//
//    @Test
//    @DisplayName("getAllOrders - returns paginated responses")
//    void getAllOrders_success() {
//        // Arrange
//        Pageable pageable = PageRequest.of(0, 10);
//        Order order = new Order();
//        Page<Order> orderPage = new PageImpl<>(List.of(order));
//
//        when(repository.findAll(pageable)).thenReturn(orderPage);
//        when(mapper.toResponse(order)).thenReturn(OrderResponse.builder().orderId(1L).build());
//
//        // Act
//        Page<OrderResponse> result = service.getAllOrders(pageable);
//
//        // Assert
//        assertEquals(1, result.getContent().size());
//        verify(repository).findAll(pageable);
//    }
//}