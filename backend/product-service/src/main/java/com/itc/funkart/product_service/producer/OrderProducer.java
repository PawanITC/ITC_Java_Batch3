package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.constants.KafkaConstants;
import com.itc.funkart.product_service.dto.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

//    public void sendOrderEvent(OrderEvent event) {
//        log.info("Producing Order Event: {}", event);
//        kafkaTemplate.send(KafkaConstants.TOPIC_ORDERS, String.valueOf(event.getUserId()), event);
//    }
public void sendOrderEvent(OrderEvent event) {
    String key = String.valueOf(event.getUserId());
    log.info("Attempting to produce order event: ID={}", key);
    // .send() in Spring Boot 3.3.5 returns CompletableFuture<SendResult<K, V>>
    kafkaTemplate.send(KafkaConstants.TOPIC_ORDERS, key, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(key, result);
                } else {
                    handleFailure(key, event, ex);
                }
            });
}

    private void handleSuccess(String key, SendResult<String, OrderEvent> result) {
        log.info("Successfully sent Order {} to partition {} at offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, OrderEvent event, Throwable ex) {
        log.error("Unable to send Order {} due to : {}", key, ex.getMessage());
        // Phase 3: You would typically trigger a Retry or save to a Dead Letter Table here
    }
}
