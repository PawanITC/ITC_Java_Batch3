package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.dto.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void sendOrderEvent(OrderEvent event) {
        log.info("Producing Order Event: {}", event);
        kafkaTemplate.send("order-topic", event);
    }
}
