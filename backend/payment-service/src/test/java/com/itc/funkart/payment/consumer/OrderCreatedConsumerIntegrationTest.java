package com.itc.funkart.payment.consumer;

import com.itc.funkart.payment.dto.event.OrderCreatedEvent;
import com.itc.funkart.payment.entity.Payment;
import com.itc.funkart.payment.repository.PaymentRepository;
import com.itc.funkart.payment.service.StripeService;
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

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <h2>OrderCreatedConsumer Integration Test</h2>
 * Validates that the service correctly listens to Kafka topics and
 * triggers the payment confirmation flow.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order_created_topic"})
public class OrderCreatedConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private StripeService stripeService;

    @Test
    @DisplayName("📥 Should trigger payment confirmation when OrderCreatedEvent is received")
    void shouldProcessOrderCreatedEvent() throws Exception {
        // 1. SETUP DATABASE (CRITICAL)
        // We create a "Pending" record so the service has something to find
        Payment pendingPayment = new Payment();
        pendingPayment.setUserId(1L); // Must match event.userId()
        pendingPayment.setOrderId(999L);
        pendingPayment.setAmount(5000L);
        pendingPayment.setCurrency("usd");
        pendingPayment.setStripePaymentIntentId("pi_123"); // Must match event.paymentIntentId()
        pendingPayment.setStatus("PENDING");

        paymentRepository.save(pendingPayment);

        // 2. MOCK STRIPE
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getStatus()).thenReturn("succeeded");
        when(stripeService.confirmPaymentIntent(any(), any(), any())).thenReturn(mockIntent);

        // 3. PREPARE THE EVENT
        OrderCreatedEvent event = new OrderCreatedEvent(
                999L, 1L, "Abbas", "abbas@ex.com", "ROLE_USER",
                5000L, "usd", "pi_123", "pm_123", "http://success.com"
        );

        // 4. SEND TO KAFKA
        kafkaTemplate.send("order_created_topic", event);

        // 5. WAIT AND VERIFY
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            // Now that the DB check passes, the code will finally reach this line!
            verify(stripeService, times(1))
                    .confirmPaymentIntent("pi_123", "pm_123", "http://success.com");
        });
    }
}