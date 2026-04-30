package com.itc.funkart.service.impl;

import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.OrderEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceImplTest {

    @Mock
    private OrderEventProducer producer;

    @InjectMocks
    private KafkaEventServiceImpl service;

    @Test
    void sendOrderEvent_success() {
        Order order = new Order();
        order.setId(1L);

        assertTrue(service.sendOrderEvent(order));
        verify(producer).publishOrderCreated(order);
    }

    @Test
    void sendOrderEvent_exceptionBranch() {

        Order order = new Order();
        order.setId(1L);

        doThrow(new RuntimeException("Kafka down"))
                .when(producer).publishOrderCreated(order);

        boolean result = service.sendOrderEvent(order);

        assertFalse(result);
    }
}