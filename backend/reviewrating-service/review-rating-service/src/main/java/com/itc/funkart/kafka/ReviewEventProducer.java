package com.itc.funkart.kafka;
import com.itc.funkart.event.ReviewEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private static final String TOPIC = "review.events.v1";

    public void send(ReviewEventEnvelope envelope) {
        byte[] bytes = envelope.getEventData().array();
        kafkaTemplate.send(TOPIC, envelope.getEventId(), bytes);
        log.info("Sent event {} to Kafka", envelope.getEventId());
    }
}