CREATE TABLE review_outbox (
                               id UUID PRIMARY KEY,
                               event_type VARCHAR(50),
                               payload JSONB,
                               created_at TIMESTAMP DEFAULT NOW(),
                               processed BOOLEAN DEFAULT FALSE
);
