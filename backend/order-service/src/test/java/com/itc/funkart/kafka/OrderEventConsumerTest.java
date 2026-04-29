package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.Order;
import com.itc.funkart.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private Acknowledgment ack;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void handleProductEvents_shouldUpdateOrderStatus_whenOrderExists() {
        UUID orderId = UUID.randomUUID();
        Order existing = Order.builder().orderId(orderId).orderStatus("CREATED").build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        OrderEvent event = OrderEvent.builder()
                .eventType("product.reserved")
                .orderId(orderId)
                .build();

        consumer.handleProductEvents(event, "corr", ack);

        assertEquals("PRODUCT_RESERVED", existing.getOrderStatus());
        verify(orderRepository).save(existing);
        verify(ack).acknowledge();
    }

    @Test
    void handlePaymentEvents_shouldConfirmOnSuccess() {
        UUID orderId = UUID.randomUUID();
        Order existing = Order.builder().orderId(orderId).orderStatus("PENDING").build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        OrderEvent event = OrderEvent.builder()
                .eventType("PAYMENT_SUCCESS")
                .orderId(orderId)
                .build();

        consumer.handlePaymentEvents(event, null, ack);

        assertEquals("CONFIRMED", existing.getOrderStatus());
        verify(orderRepository).save(existing);
        verify(ack).acknowledge();
    }

    @Test
    void handlePaymentEvents_shouldFailOnFailed() {
        UUID orderId = UUID.randomUUID();
        Order existing = Order.builder().orderId(orderId).orderStatus("PENDING").build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existing));

        OrderEvent event = OrderEvent.builder()
                .eventType("PAYMENT_FAILED")
                .orderId(orderId)
                .build();

        consumer.handlePaymentEvents(event, "corr", ack);

        assertEquals("FAILED", existing.getOrderStatus());
        verify(orderRepository).save(existing);
        verify(ack).acknowledge();
    }

    @Test
    void handlePaymentEvents_shouldAckEvenWhenOrderMissing() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        OrderEvent event = OrderEvent.builder()
                .eventType("PAYMENT_SUCCESS")
                .orderId(orderId)
                .build();

        consumer.handlePaymentEvents(event, "corr", ack);

        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any(Order.class));
        verify(ack).acknowledge();
    }
}

