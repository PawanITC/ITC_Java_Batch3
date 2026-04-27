package com.itc.funkart.outbox;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.itc.funkart.event.ReviewEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review_outbox")
@Getter
@NoArgsConstructor
public class OutboxEntity {

    @Id
    private UUID id;

    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private Instant createdAt;

    private boolean processed;

    public OutboxEntity(ReviewEvent event) {
        try {
            this.id = UUID.randomUUID();
            this.eventType = event.getEventType();
            this.payload = new ObjectMapper().writeValueAsString(event);
            this.createdAt = Instant.now();
            this.processed = false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }

    public void markProcessed() {
        this.processed = true;
    }

    public ReviewEvent toEvent() {
        try {
            return new ObjectMapper().readValue(payload, ReviewEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}

