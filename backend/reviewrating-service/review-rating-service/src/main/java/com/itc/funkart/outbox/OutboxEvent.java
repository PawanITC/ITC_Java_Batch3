package com.itc.funkart.outbox;



import com.itc.funkart.event.ReviewEventEnvelope;
import jakarta.persistence.*;

import java.nio.ByteBuffer;
import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private Instant occurredAt;

    @Lob
    @Column(nullable = false)
    private byte[] payload;   // Avro serialized bytes

    @Column(nullable = false)
    private boolean processed = false;

    public OutboxEvent() {}

    public OutboxEvent(String id, String eventType, Instant occurredAt, byte[] payload) {
        this.id = id;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.processed = false;
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void markProcessed() {
        this.processed = true;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    public ReviewEventEnvelope toEnvelope() {
        return ReviewEventEnvelope.newBuilder()
                .setEventId(this.id)
                .setEventType(this.eventType)
                .setOccurredAt(this.occurredAt.toString())
                .setEventData(ByteBuffer.wrap(this.payload))
                .build();
    }

}

