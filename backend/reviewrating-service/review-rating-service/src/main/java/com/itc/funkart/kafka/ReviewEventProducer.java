package com.itc.funkart.kafka;


import com.itc.funkart.config.KafkaTopicConfig;
import com.itc.funkart.event.ReviewCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ReviewEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.review-created}")
    private String topic;

    public ReviewEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendReviewCreatedEvent(ReviewCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("ReviewCreatedEvent cannot be null");
        }
        // FIX 1: Convert Long → String
        String key = String.valueOf(event.getProductId());

        // FIX 2: Use correct getter
        log.info("Sending ReviewCreatedEvent for productId={} reviewId={}",
                event.getProductId(), event.getReviewId());

        kafkaTemplate.send(topic, key, event);

    }
    public void publishReviewEvent(ReviewCreatedEvent event) {
        sendReviewCreatedEvent(event);
    }
}

