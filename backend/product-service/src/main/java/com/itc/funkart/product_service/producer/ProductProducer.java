package com.itc.funkart.product_service.producer;

import com.itc.funkart.product_service.constants.KafkaConstants;
import com.itc.funkart.product_service.dto.events.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductProducer {

    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;

//    public void sendMessage(ProductEvent event) {
//        log.info("Producing event: {}", event);
//        kafkaTemplate.send("products", String.valueOf(event.getId()), event);
//    }
public void sendMessage(ProductEvent event) {
    String key = String.valueOf(event.getId());
    log.info("Attempting to produce product event: ID={}", key);

    // .send() in Spring Boot 3.3.5 returns CompletableFuture<SendResult<K, V>>
    kafkaTemplate.send(KafkaConstants.TOPIC_PRODUCTS, key, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    handleSuccess(key, result);
                } else {
                    handleFailure(key, event, ex);
                }
            });
}

    private void handleSuccess(String key, SendResult<String, ProductEvent> result) {
        log.info("Successfully sent Product {} to partition {} at offset {}",
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    private void handleFailure(String key, ProductEvent event, Throwable ex) {
        log.error("Unable to send Product {} due to : {}", key, ex.getMessage());
        // Phase 3: You would typically trigger a Retry or save to a Dead Letter Table here
        // Save to a table called 'failed_events'
//        failedEventRepository.save(new FailedEventEntity(
//                "products",
//                key,
//                event.toString(),
//                ex.getMessage()
//        ));
    }
}
