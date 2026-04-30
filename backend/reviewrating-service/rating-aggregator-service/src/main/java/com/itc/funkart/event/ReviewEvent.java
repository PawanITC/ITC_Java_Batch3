package com.itc.funkart.event;



import java.time.Instant;
import java.util.UUID;

public abstract class ReviewEvent {

    private final String eventId;
    private final String eventType;
    private final Instant occurredAt;

    protected ReviewEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
}

