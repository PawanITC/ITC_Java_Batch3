package com.itc.funkart.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false ORDER BY o.occurredAt ASC")
    List<OutboxEvent> findUnprocessed();
}
