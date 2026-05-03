package com.itc.funkart.service.impl;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.dto.event.order.OrderInitiatedEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.entity.Order;
import com.itc.funkart.kafka.producer.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.service.KafkaEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <h2>KafkaEventServiceImpl</h2>
 * <p>
 * Implementation of the event dispatcher. It leverages the <b>JVM Heap</b> to
 * snapshot entity state into immutable Records before the database session closes.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventServiceImpl implements KafkaEventService {

    private final OrderEventProducer producer;
    private final OrderMapper mapper;

    @Override
    public void sendOrderEvent(Order order, OrderEventType eventType) {
        // We map to a Record immediately to capture a "Point-in-Time" snapshot
        OrderEvent event = mapper.toEvent(order, eventType);
        syncAndSend(() -> producer.publishOrderEvent(event), event.orderId().toString());
    }

    @Override
    public void sendOrderInitiated(OrderInitiatedEvent event) {
        // Keying by User ID for initiation phase if Order ID is not yet generated
        syncAndSend(() -> producer.publishOrderCreated(event), event.userId().toString());
    }

    @Override
    public void sendOrderCancelled(OrderCancelledEvent event) {
        syncAndSend(() -> producer.publishOrderCancelled(event), event.orderId().toString());
    }

    /**
     * <h3>Transaction Guard</h3>
     * <p>
     * Wraps the Kafka dispatch logic in a Spring Transaction Sync.
     * The {@code publishTask} will execute only after the current database
     * transaction has entered the {@code afterCommit} phase.
     * </p>
     *
     * @param publishTask The lambda containing the specific producer call.
     * @param identifier  A correlation ID (Order or User ID) for logging.
     */
    private void syncAndSend(Runnable publishTask, String identifier) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("🔗 Registering post-commit hook for ID: {}", identifier);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        publishTask.run();
                    } catch (Exception e) {
                        // Crucial: The DB is committed, so we log as ERROR for manual review
                        log.error("❌ Kafka dispatch failed for [ID: {}] after DB commit: {}",
                                identifier, e.getMessage());
                    }
                }
            });
        } else {
            // Safe fallback if called outside of a @Transactional block
            log.warn("⚠️ No active transaction found. Dispatching event for [{}] immediately.", identifier);
            publishTask.run();
        }
    }

    public void syncEvent(Order order) {
        // Determine the event type based on the Business Status
        OrderEventType type = switch(order.getStatus()) {
            case PENDING   -> OrderEventType.ORDER_INITIATED;
            case PAID      -> OrderEventType.PAYMENT_SUCCESS;
            case SHIPPED   -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            case CANCELLED -> OrderEventType.ORDER_CANCELLED;
            case REFUNDED  -> OrderEventType.ORDER_REFUNDED;
            // No 'default' needed if all enum members are covered
        };

        // Dispatch via your transaction-aware logic
        sendOrderEvent(order, type);
    }
}