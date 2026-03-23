package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish user signup event to Kafka
     */
    public void publishUserSignupEvent(UserSignupEvent event) {
        try {
            kafkaTemplate.send("user-signup", event.getUserId().toString(), event);
            logger.info("✓ Published user-signup event for user: {}", event.getUserId());
        } catch (Exception ex) {
            logger.error("✗ Failed to publish user-signup event: {}", ex.getMessage());
        }
    }

    /**
     * Publish user login event to Kafka
     */
    public void publishUserLoginEvent(UserLoginEvent event){
        try {
            kafkaTemplate.send("user-login", event.getUserId().toString(), event);
            logger.info("✓ Published user-login event for user: {}", event.getUserId());
        } catch (Exception ex) {
            logger.error("✗ Failed to publish user-login event: {}", ex.getMessage());
        }
    }
}
