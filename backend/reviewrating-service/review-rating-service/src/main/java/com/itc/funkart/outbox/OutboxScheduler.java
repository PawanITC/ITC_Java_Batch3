package com.itc.funkart.outbox;



import com.itc.funkart.kafka.ReviewEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final ReviewEventProducer kafkaProducer;

    @Scheduled(fixedDelay = 500)
    public void publishOutboxEvents() {

        List<OutboxEvent> events = outboxRepository.findUnprocessed();

        if (events.isEmpty()) {
            return;
        }

        log.info("OutboxScheduler: Found {} unprocessed events", events.size());

        events.forEach(e -> {
            try {
                kafkaProducer.send(e.toEnvelope());
                e.markProcessed();
                outboxRepository.save(e);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}", e.getId(), ex);
            }
        });
    }
}
