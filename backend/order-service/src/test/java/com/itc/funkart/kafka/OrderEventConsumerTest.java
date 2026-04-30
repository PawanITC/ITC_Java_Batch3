package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.entity.OrderStatus;
import com.itc.funkart.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void consume_orderCreated_branch() {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_CREATED");

        consumer.consume(event);

        verifyNoInteractions(orderService);
    }

    @Test
    void consume_orderUpdated_branch() {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_UPDATED");

        consumer.consume(event);

        verifyNoInteractions(orderService);
    }

    @Test
    void consume_orderCancelled_branch() {
        OrderEvent event = new OrderEvent();
        event.setEventType("ORDER_CANCELLED");
        event.setOrderId(1L);

        consumer.consume(event);

        verify(orderService).updateOrderStatus(1L, OrderStatus.CANCELLED);
    }

    @Test
    void consume_unknownBranch() {
        OrderEvent event = new OrderEvent();
        event.setEventType("UNKNOWN");

        consumer.consume(event);

        verifyNoInteractions(orderService);
    }
}