package com.itc.funkart.payment.consumer;

import com.itc.funkart.payment.dto.event.OrderCreatedEvent;
import com.itc.funkart.payment.dto.jwt.JwtUserDto;
import com.itc.funkart.payment.dto.request.ConfirmPaymentRequest;
import com.itc.funkart.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer responsible for handling {@code OrderCreatedEvent} messages.
 *
 * <p>This component listens to the {@code order_created_topic} and initiates
 * the payment confirmation flow when a new order is created.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *     <li>Consume order creation events from Kafka</li>
 *     <li>Perform basic validation on incoming event payloads</li>
 *     <li>Transform event data into domain-specific DTOs</li>
 *     <li>Delegate payment processing to {@link PaymentService}</li>
 * </ul>
 *
 * <p><b>Notes:</b></p>
 * <ul>
 *     <li>This consumer assumes Kafka is a trusted internal source</li>
 *     <li>Manual validation is used instead of {@code @Valid}</li>
 *     <li>No retry or dead-letter handling is implemented here</li>
 * </ul>
 */
@Component
public class OrderCreatedConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedConsumer.class);
    private final PaymentService paymentService;

    /**
     * Constructs the consumer with required dependencies.
     *
     * @param paymentService service responsible for confirming payments
     */
    public OrderCreatedConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Kafka listener method triggered when an {@code OrderCreatedEvent} is published.
     *
     * <p>Processing flow:</p>
     * <ol>
     *     <li>Log receipt of the event</li>
     *     <li>Validate required fields</li>
     *     <li>Map event data to {@link JwtUserDto}</li>
     *     <li>Construct {@link ConfirmPaymentRequest}</li>
     *     <li>Invoke {@link PaymentService#confirmPayment(JwtUserDto, ConfirmPaymentRequest)}</li>
     * </ol>
     *
     * @param event the incoming order creation event from Kafka
     */
    @KafkaListener(topics = "order_created_topic", groupId = "payment-service-group")
    public void onOrderCreated(OrderCreatedEvent event) {

        logger.info("Received order_created_event {} for user {}", event.orderId(), event.userId());

        // Manual validation check (replacement for @Valid)
        if (event.paymentIntentId() == null || event.paymentMethodId() == null) {
            logger.error("Rejecting malformed Kafka event for Order: {}", event.orderId());
            return;
        }

        /*
         * Hydrate user context from event payload.
         * Assumes event data is trustworthy (internal system communication).
         */
        JwtUserDto user = JwtUserDto.builder()
                .id(event.userId())
                .name(event.userName())
                .email(event.userEmail())
                .role(event.userRole())
                .build();

        /*
         * Build payment confirmation request equivalent to HTTP controller input.
         */
        ConfirmPaymentRequest request = new ConfirmPaymentRequest(
                event.paymentIntentId(),
                event.paymentMethodId(),
                event.returnUrl()
        );

        /*
         * Delegate to application service layer.
         */
        paymentService.confirmPayment(user, request);
    }
}