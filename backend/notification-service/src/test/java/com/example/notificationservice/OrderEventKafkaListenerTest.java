package com.example.notificationservice;

import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.event.OrderStatus;
import com.example.notificationservice.kafka.OrderEventKafkaListener;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventKafkaListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventKafkaListener listener;

    @Test
    void listen_validJson_delegatesToNotificationService() throws Exception {
        String payload = """
            {
                "orderId": "123",
                "email": "test@gmail.com",
                "phone": "123456789",
                "status": "DELIVERED"
            }
            """;

        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId("123");
        event.setEmail("test@gmail.com");
        event.setPhone("123456789");
        event.setStatus(OrderStatus.DELIVERED);

        when(objectMapper.readValue(payload, OrderEventDTO.class)).thenReturn(event);

        listener.listen(payload);

        verify(objectMapper, times(1)).readValue(payload, OrderEventDTO.class);
        verify(notificationService, times(1)).processOrderEvent(event);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void listen_invalidJson_doesNotCallNotificationService() throws Exception {
        String payload = "{ invalid json }";

        when(objectMapper.readValue(eq(payload), eq(OrderEventDTO.class)))
                .thenThrow(new RuntimeException("Bad JSON"));

        listener.listen(payload);

        verify(objectMapper, times(1)).readValue(payload, OrderEventDTO.class);
        verify(notificationService, never()).processOrderEvent(any());
    }

    @Test
    void listen_whenNotificationServiceThrows_exceptionIsSwallowed() throws Exception {
        String payload = """
            {
                "orderId": "123",
                "email": "test@gmail.com",
                "phone": "123456789",
                "status": "DELIVERED"
            }
            """;

        OrderEventDTO event = new OrderEventDTO();
        event.setOrderId("123");
        event.setEmail("test@gmail.com");
        event.setPhone("123456789");
        event.setStatus(OrderStatus.DELIVERED);

        when(objectMapper.readValue(payload, OrderEventDTO.class)).thenReturn(event);
        doThrow(new RuntimeException("Service failed"))
                .when(notificationService)
                .processOrderEvent(event);

        assertDoesNotThrow(() -> listener.listen(payload));

        verify(notificationService, times(1)).processOrderEvent(event);
    }
}