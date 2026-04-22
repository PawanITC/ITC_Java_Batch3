package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.dto.events.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

    public void sendMessage(ProductEvent event) {
        log.info("Producing event: {}", event);
        kafkaTemplate.send("product-topic", event);
    }
}
