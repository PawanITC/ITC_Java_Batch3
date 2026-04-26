package com.itc.funkart.payment.consumer;

import com.itc.funkart.payment.dto.event.OrderCreatedEvent;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedConsumer.class);
    private final PaymentService paymentService;

    public OrderCreatedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order_created_topic", groupId = "payment-service-group")
    public void onOrderCreated(OrderCreatedEvent event) {
        logger.info("Received order_created_event {} for user {}", event.orderId(), event.userId());

        // Manual validation check (The replacement for @Valid)
        if (event.paymentIntentId() == null || event.paymentMethodId() == null) {
            logger.error("Rejecting malformed Kafka event for Order: {}", event.orderId());
            return; // Don't process garbage data
        }

        // 1. We manually hydrate the JwtUserDto because Kafka is an internal trusted source
        JwtUserDto user = JwtUserDto.builder()
                .id(event.userId())
                .name(event.userName())
                .email(event.userEmail())
                .role(event.userRole())
                .build();

        // 2. Build the request exactly like our controller would
        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
                event.paymentIntentId(), event.paymentMethodId(), event.returnUrl());

        // 3. Call my existing services
        paymentService.confirmPayment(user, request);
    }
}
