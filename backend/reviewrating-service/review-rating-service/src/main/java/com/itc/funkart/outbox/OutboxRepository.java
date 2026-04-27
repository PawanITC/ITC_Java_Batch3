package com.itc.funkart.outbox;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT o FROM OutboxEntity o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnprocessed();
}

