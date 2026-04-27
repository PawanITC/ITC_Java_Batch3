package com.itc.funkart.event;


import java.time.Instant;

public abstract class ReviewEvent {
    private final String eventId;
    private final String eventType;
    private final Instant occurredAt;

    protected ReviewEvent(String eventId, String eventType, Instant occurredAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
    }

    // getters

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}

