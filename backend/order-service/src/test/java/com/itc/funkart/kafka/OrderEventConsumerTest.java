//package com.itc.funkart.kafka;
//
//import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
//import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
//import com.itc.funkart.common.enums.order.OrderEventType;
//import com.itc.funkart.common.enums.order.OrderStatus;
//import com.itc.funkart.kafka.consumer.OrderEventConsumer;
//import com.itc.funkart.service.OrderService;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
///**
// * <h2>OrderEventConsumerTest</h2>
// * <p>
// * Verifies that the Kafka consumer correctly routes specialized events
// * to the appropriate business logic in the {@link OrderService}.
// * </p>
// *
// * <h3>Testing Strategy:</h3>
// * <ul>
// *   <li><b>Type-Safe Routing:</b> Ensures the {@code @KafkaHandler} methods
// *   delegate to the correct service methods.</li>
// *   <li><b>Resiliency:</b> Confirms that the consumer catches service-layer
// *   exceptions to prevent Kafka partition blocking.</li>
// *   <li><b>Memory Isolation:</b> Uses Mocks to isolate the transport layer
// *   from the database (Heap efficiency).</li>
// * </ul>
// */
//@ExtendWith(MockitoExtension.class)
//@ActiveProfiles("test")
//class OrderEventConsumerTest {
//
//    @Mock
//    private OrderService orderService;
//
//    @InjectMocks
//    private OrderEventConsumer consumer;
//
//    @Test
//    @DisplayName("Should trigger order creation when OrderInitiatedEvent is received")
//    void handleOrderInitiated_Success() {
//        // Arrange: Updated to match your Record(eventType, orderId, userId, totalAmount, productIds)
//        OrderInitiatedEvent event = new OrderInitiatedEvent(
//                OrderEventType.ORDER_INITIATED,
//                2001L, // orderId (Newly added for traceability)
//                100L,  // userId
//                new BigDecimal("250.00"),
//                List.of(),
//                "usd", null
//        );
//
//        // Act
//        consumer.handleCheckoutInitiated(event);
//
//        // Assert: Ensure the service was called with the exact Record snapshot
//        verify(orderService, times(1)).processOrderInitiation(event);
//    }
//
//    @Test
//    @DisplayName("Should update status to CANCELLED when OrderCancelledEvent is received")
//    void handleOrderCancelled_Success() {
//        // Arrange
//        OrderCancelledEvent event = OrderCancelledEvent.builder()
//                .orderId(500L)
//                .userId(100L)
//                .eventType(OrderEventType.ORDER_CANCELLED)
//                .reason("Customer changed mind")
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        // Act
//        consumer.handleOrderCancelled(event);
//
//        // Assert: Confirm status routing logic
//        verify(orderService, times(1)).updateOrderStatus(500L, OrderStatus.CANCELLED);
//    }
//
//    @Test
//    @DisplayName("Should not crash when service throws an exception")
//    void handleOrderInitiated_ErrorHandling() {
//        // Arrange
//        OrderInitiatedEvent event = new OrderInitiatedEvent(
//                OrderEventType.ORDER_INITIATED, 2002L, 1L, BigDecimal.TEN, List.of(),"usd",null
//        );
//        doThrow(new RuntimeException("Database Connection Timeout")).when(orderService).processOrderInitiation(any());
//
//        // Act & Assert: This shouldn't throw an exception to the listener container
//        // due to the internal try-catch in the consumer.
//        consumer.handleCheckoutInitiated(event);
//
//        verify(orderService).processOrderInitiation(event);
//    }
//
//    @Test
//    @DisplayName("Should handle unknown event types gracefully")
//    void handleUnknown_Success() {
//        // Act: Simulate an object type the consumer doesn't have a specific handler for
//        consumer.handleUnknown("Malformed JSON or unsupported event type");
//
//        // Assert: No business logic should be executed
//        verifyNoInteractions(orderService);
//    }
//}