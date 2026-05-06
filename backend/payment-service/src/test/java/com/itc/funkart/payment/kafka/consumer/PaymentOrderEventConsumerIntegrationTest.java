package com.itc.funkart.payment.kafka.consumer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.CHECKOUT_INITIATED},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class PaymentOrderEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private StripeService stripeService;

    @Test
    @DisplayName("Should pre-create PaymentIntent on ORDER_INITIATED")
    void shouldProcessInitiatedEvent() throws StripeException {

        paymentRepository.deleteAll();

        PaymentIntent mockPI = mock(PaymentIntent.class);
        when(mockPI.getId()).thenReturn("pi_mock_123");

        when(stripeService.createPaymentIntent(
                anyLong(), anyString(), anyLong(), anyLong()
        )).thenReturn(mockPI);

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", 123L);
        payload.put("customerId", 4L);
        payload.put("totalAmount", 360.00);
        payload.put("currency", "usd");

        kafkaTemplate.send(KafkaTopics.CHECKOUT_INITIATED, payload);

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    var payment = paymentRepository.findByOrderId(123L)
                            .orElseThrow();

                    assertEquals("PENDING", payment.getStatus());
                    assertEquals("pi_mock_123", payment.getStripePaymentIntentId());
                    assertEquals(36000L, payment.getAmount());

                    verify(stripeService, times(1))
                            .createPaymentIntent(
                                    eq(36000L),
                                    eq("usd"),
                                    eq(4L),
                                    anyLong()
                            );
                });
    }
}