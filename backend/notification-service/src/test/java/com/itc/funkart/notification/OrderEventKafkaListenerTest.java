package com.itc.funkart.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.notification.event.OrderStatus;
import com.itc.funkart.notification.kafka.OrderEventKafkaListener;
import com.itc.funkart.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrderEventKafkaListener}.
 *
 * <p>Uses a real {@link ObjectMapper} (via {@code @Spy}) so that
 * {@code readTree} / {@code treeToValue} work naturally without
 * complex JsonNode mock chaining. Only {@link NotificationService}
 * is mocked to isolate the listener's routing logic.</p>
 *
 * <p>The listener exposes two entry points:
 * <ul>
 *   <li>{@code listenOrderEvent} — for {@code orders.events.v1}</li>
 *   <li>{@code listenPaymentEvent} — for {@code payments.events.v1}</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderEventKafkaListenerTest {

    /**
     * Real ObjectMapper so readTree / treeToValue work without stubs.
     */
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderEventKafkaListener listener;

    // ─── listenOrderEvent — legacy format (has "email" field) ─────────────────

    @Test
    void listenOrderEvent_legacyFormatWithEmail_delegatesToNotificationService() {
        // Legacy DTO shape: contains "email" field → full processOrderEvent call
        String payload = """
                {
                    "orderId": "123",
                    "email": "test@gmail.com",
                    "phone": "123456789",
                    "status": "DELIVERED"
                }
                """;

        listener.listenOrderEvent(payload);

        verify(notificationService, times(1)).processOrderEvent(argThat(dto ->
                "123".equals(dto.getOrderId())
                        && "test@gmail.com".equals(dto.getEmail())
                        && OrderStatus.DELIVERED.equals(dto.getStatus())
        ));
        verifyNoMoreInteractions(notificationService);
    }

    // ─── listenOrderEvent — new OrderEvent format (no "email" field) ──────────

    @Test
    void listenOrderEvent_newFormatOrderInitiated_skipsNotificationDueToNoEmail() {
        // New OrderEvent format from Order Service — no email field.
        // Notification is skipped (logged) because email is unavailable.
        String payload = """
                {
                    "eventType": "ORDER_INITIATED",
                    "orderId": 456,
                    "customerId": 7,
                    "totalAmount": 99.99
                }
                """;

        listener.listenOrderEvent(payload);

        // No email → processOrderEvent is never called
        verifyNoInteractions(notificationService);
    }

    @Test
    void listenOrderEvent_unknownEventType_skipsNotification() {
        String payload = """
                {
                    "eventType": "SOME_FUTURE_EVENT",
                    "orderId": 789,
                    "customerId": 1
                }
                """;

        listener.listenOrderEvent(payload);

        verifyNoInteractions(notificationService);
    }

    // ─── listenOrderEvent — error handling ────────────────────────────────────

    @Test
    void listenOrderEvent_malformedJson_exceptionIsSwallowed() {
        assertDoesNotThrow(() -> listener.listenOrderEvent("{ not valid json }"));
        verifyNoInteractions(notificationService);
    }

    @Test
    void listenOrderEvent_notificationServiceThrows_exceptionIsSwallowed() {
        String payload = """
                {
                    "orderId": "123",
                    "email": "test@gmail.com",
                    "phone": "123456789",
                    "status": "ORDER_CONFIRMED"
                }
                """;

        doThrow(new RuntimeException("SMTP down"))
                .when(notificationService)
                .processOrderEvent(any());

        assertDoesNotThrow(() -> listener.listenOrderEvent(payload));
        verify(notificationService, times(1)).processOrderEvent(any());
    }

    // ─── listenPaymentEvent ───────────────────────────────────────────────────

    @Test
    void listenPaymentEvent_validPayload_logsAndSkipsNotification() {
        // Payment event carries userId/orderId but no email — notification deferred.
        String payload = """
                {
                    "paymentId": 1,
                    "orderId": 123,
                    "userId": 4,
                    "stripeId": "pi_mock",
                    "amount": 36000,
                    "timestamp": 1778268414025
                }
                """;

        assertDoesNotThrow(() -> listener.listenPaymentEvent(payload));
        // Email resolution deferred → no processOrderEvent call
        verifyNoInteractions(notificationService);
    }

    @Test
    void listenPaymentEvent_malformedJson_exceptionIsSwallowed() {
        assertDoesNotThrow(() -> listener.listenPaymentEvent("{ bad }"));
        verifyNoInteractions(notificationService);
    }
}
