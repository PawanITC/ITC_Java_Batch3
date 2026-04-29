
package com.itc.funkart.kafka;

import com.itc.funkart.dto.OrderEvent;
import com.itc.funkart.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    // ================= PRODUCT EVENTS =================
    @KafkaListener(
            topics = "products.events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductEvents(
            OrderEvent event,
            @Header(value = "correlationId", required = false) String correlationId,
            Acknowledgment ack) {

        MDC.put("correlationId", correlationId != null ? correlationId : "N/A");

        try {

            log.info(" Product Event Received: {}", event);

            log.info("Product EventType: {} | OrderId: {}",
                    event.getEventType(),
                    event.getOrderId());

            // TODO: update order based on product event
            // Example:
             orderRepository.findById(event.getOrderId()).ifPresent(order -> {
                 order.setOrderStatus("PRODUCT_RESERVED");
                 orderRepository.save(order);
             });

            ack.acknowledge();
            log.info(" Product event processed & acknowledged");

        } catch (Exception e) {
            log.error(" Failed to process product event", e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
        }
    }

    // ================= PAYMENT EVENTS =================
    @KafkaListener(
            topics = "payments.events",
            groupId = "order-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(
            OrderEvent event,
            @Header(value = "correlationId", required = false) String correlationId,
            Acknowledgment ack) {

        MDC.put("correlationId", correlationId != null ? correlationId : "N/A");

        try {

            log.info(" Payment Event Received: {}", event);

            log.info("Payment EventType: {} | OrderId: {}",
                    event.getEventType(),
                    event.getOrderId());

            // TODO: update order status
            // Example:
            orderRepository.findById(event.getOrderId())
                    .ifPresentOrElse(order -> {

                        String eventType = event.getEventType();

                        if ("PAYMENT_SUCCESS".equals(eventType)) {
                            order.setOrderStatus("CONFIRMED");
                        }
                        else if ("PAYMENT_FAILED".equals(eventType)) {
                            order.setOrderStatus("FAILED");
                        }
                        else {
                            order.setOrderStatus("PENDING");
                        }

                        orderRepository.save(order);

                        log.info("Order updated successfully | orderId={} | status={}",
                                order.getOrderId(),
                                order.getOrderStatus());

                    }, () -> {
                        log.warn(" Order not found for payment event | orderId={}",
                                event.getOrderId());
                    });

            ack.acknowledge();
            log.info(" Payment event processed & acknowledged");

        } catch (Exception e) {
            log.error(" Failed to process payment event", e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}