package com.itc.funkart.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.event.ReviewEvent;
import com.itc.funkart.event.ReviewEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
@RequiredArgsConstructor
public class ReviewEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(ReviewEventEnvelope  event) {
        ReviewEventEnvelope envelope = envelope(event);
        kafkaTemplate.send("review.events.v1", envelope.getEventId(), envelope);
    }

    private ReviewEventEnvelope envelope(ReviewEventEnvelope  event) {
        return ReviewEventEnvelope.newBuilder()
                .setEventId(event.getEventId())
                .setOccurredAt(event.getOccurredAt().toString())   // ✔ String
                .setEventType(event.getEventType())
                .setEventData(ByteBuffer.wrap(serialize(event)))   // ✔ ByteBuffer
                .build();
    }

    private byte[] serialize(ReviewEventEnvelope  event) {
        try {
            return new ObjectMapper().writeValueAsBytes(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
