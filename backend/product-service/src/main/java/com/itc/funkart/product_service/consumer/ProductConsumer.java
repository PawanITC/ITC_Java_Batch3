package com.itc.funkart.product_service.consumer;

import com.itc.funkart.product_service.dto.events.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductConsumer {

    @KafkaListener(topics = "product-topic", groupId = "product-group")
    public void consume(ProductEvent event) {
        log.info(">>>> EVENT RECEIVED: {}", event);
    }
}
