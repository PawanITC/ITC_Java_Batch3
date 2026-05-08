package com.itc.funkart.kafka.producer;

import com.itc.funkart.common.constants.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String productId, Object event) {
        kafkaTemplate.send(KafkaTopics.REVIEWS, productId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish review event for productId={}: {}", productId, ex.getMessage());
                    } else {
                        log.debug("Published review event for productId={} offset={}",
                                productId, result.getRecordMetadata().offset());
                    }
                });
    }
}
