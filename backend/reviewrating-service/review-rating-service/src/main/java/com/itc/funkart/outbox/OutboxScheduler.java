package com.itc.funkart.outbox;

import com.itc.funkart.event.ReviewEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final String TOPIC = "review.events.v1";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {

        List<OutboxEvent> events = outboxRepository.findUnprocessed();

        for (OutboxEvent event : events) {
            try {
                ReviewEventEnvelope envelope = event.toEnvelope();
                ByteBuffer data = envelope.getEventData();
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);

                ProducerRecord<String, byte[]> record =
                        new ProducerRecord<>(TOPIC, event.getId(), bytes);

                kafkaTemplate.send(record).get();

                event.markProcessed();
                outboxRepository.save(event);

                log.info("Published outbox event {} to Kafka", event.getId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
                // leave as unprocessed
            }
        }
    }
}
