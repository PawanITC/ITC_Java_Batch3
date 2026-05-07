package com.itc.funkart.consumer;

import com.example.events.common.ReviewPayload;
import com.itc.funkart.avro.ReviewCreatedEvent;
import com.itc.funkart.avro.ReviewDeletedEvent;

import com.itc.funkart.event.ReviewEventEnvelope;
import com.itc.funkart.event.ReviewUpdatedEvent;
import com.itc.funkart.kafka.AvroDeserializer;
import com.itc.funkart.service.RatingSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private final AvroDeserializer avroDeserializer;
    private final RatingSummaryService ratingSummaryService;

    @KafkaListener(topics = "review.events.v1", groupId = "rating-aggregator")
    public void consume(byte[] message) {

        ReviewEventEnvelope envelope = avroDeserializer.deserializeEnvelope(message);

        String type = envelope.getEventType();
        ByteBuffer data = envelope.getEventData();
        byte[] payloadBytes = new byte[data.remaining()];
        data.get(payloadBytes);

        Long productId = null;

        try {
            switch (type) {
                case "ReviewCreatedEvent" -> {
                    SpecificDatumReader<ReviewCreatedEvent> reader =
                            new SpecificDatumReader<>(ReviewCreatedEvent.class);
                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payloadBytes, null);
                    ReviewCreatedEvent event = reader.read(null, decoder);
                    ReviewPayload p = event.getPayload();
                    productId = p.getProductId();
                }
                case "ReviewUpdatedEvent" -> {
                    SpecificDatumReader<com.itc.funkart.event.ReviewUpdatedEvent> reader =
                            new SpecificDatumReader<>(com.itc.funkart.event.ReviewUpdatedEvent.class);
                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payloadBytes, null);
                    com.itc.funkart.event.ReviewUpdatedEvent event = reader.read(null, decoder);
                    ReviewPayload p = event.getPayload();
                    productId = p.getProductId();
                }
                case "ReviewDeletedEvent" -> {
                    SpecificDatumReader<ReviewDeletedEvent> reader =
                            new SpecificDatumReader<>(ReviewDeletedEvent.class);
                    BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payloadBytes, null);
                    ReviewDeletedEvent event = reader.read(null, decoder);
                    ReviewPayload p = event.getPayload();
                    productId = p.getProductId();
                }
                default -> log.warn("Unknown event type: {}", type);
            }

            if (productId != null) {
                ratingSummaryService.recalculateSummary(productId);
            }
        } catch (Exception e) {
            log.error("Failed to process event type {}", type, e);
        }
    }
}
