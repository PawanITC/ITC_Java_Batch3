package com.example.notificationservice.kafka;
import com.example.notificationservice.dto.OrderEventDTO;
import com.example.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public OrderEventKafkaListener(ObjectMapper objectMapper,
                                   NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void listen(String payload) {
        try {
            OrderEventDTO event = objectMapper.readValue(payload, OrderEventDTO.class);
            log.info("Kafka order event received for orderId={}", event.getOrderId());
            notificationService.processOrderEvent(event);
        } catch (Exception ex) {
            log.error("Failed to process Kafka message: {}", payload, ex);
        }
    }
}