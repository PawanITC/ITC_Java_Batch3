package com.itc.funkart.aggregator.service.impl;

import com.itc.funkart.common.dto.event.order.OrderCancelledEvent;
import com.itc.funkart.common.dto.event.order.OrderEvent;
import com.itc.funkart.common.enums.order.OrderEventType;
import com.itc.funkart.aggregator.entity.Order;
import com.itc.funkart.aggregator.kafka.producer.OrderEventProducer;
import com.itc.funkart.mapper.OrderMapper;
import com.itc.funkart.aggregator.service.KafkaEventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <h2>KafkaEventServiceImpl</h2>
 *
 * <h3>Fix applied:</h3>
 * <p>{@code mapper.toInitiatedEvent(order)} does not exist on {@link com.itc.funkart.mapper.OrderMapper}.
 * The mapper only exposes {@code toEvent(Order, OrderEventType)} which returns an {@link com.itc.funkart.common.dto.event.order.OrderEvent}.
 * Changed {@code sendOrderCreated} to call {@code mapper.toEvent(order, ORDER_INITIATED)}
 * and dispatch via {@code producer.publishOrderEvent(event)}.</p>
 *
 * <p>{@code publishOrderInitiated(OrderInitiatedEvent)} was also removed from the producer
 * because publishing an {@code OrderInitiatedEvent} to the {@code ORDERS} topic is semantically
 * wrong — that topic carries {@code OrderEvent} records. Notification Service consumers
 * would receive an unexpected type.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventServiceImpl implements KafkaEventService {

    private final OrderEventProducer producer;
    private final OrderMapper mapper;

    @Override
    public void sendOrderEvent(Order order, OrderEventType eventType) {
        var event = mapper.toEvent(order, eventType);
        String key = order.getId() != null
                ? order.getId().toString()
                : order.getCustomerId().toString();

        syncAndSend(() -> producer.publishOrderEvent(event), key);
    }

    /**
     * Maps the persisted {@link Order} entity → {@code OrderEvent} and publishes
     * to the {@code ORDERS} topic post-commit.
     *
     * <h3>Fix:</h3>
     * <p>Was calling {@code mapper.toInitiatedEvent(order)} which doesn't exist.
     * Corrected to {@code mapper.toEvent(order, ORDER_INITIATED)} which is the
     * existing mapper method that returns an {@code OrderEvent}.</p>
     */
    @Override
    @Transactional
    public void sendOrderCreated(Order order) {
        OrderEvent event = mapper.toEvent(order, OrderEventType.ORDER_INITIATED);

        producer.publishOrderEvent(event); // direct publish
    }

    @Override
    public void sendOrderCancelled(OrderCancelledEvent event) {
        syncAndSend(() -> producer.publishOrderCancelled(event), event.orderId().toString());
    }

    /**
     * Transaction guard — Kafka fires only after DB commit.
     * Falls back to immediate dispatch outside a transaction (e.g. in unit tests).
     */
    private void syncAndSend(Runnable publishTask, String identifier) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("🔗 Registering post-commit Kafka hook for ID: {}", identifier);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        publishTask.run();
                    } catch (Exception e) {
                        log.error("❌ Kafka dispatch failed after DB commit [ID: {}]: {}",
                                identifier, e.getMessage());
                    }
                }
            });
        } else {
            log.warn("⚠️ No active transaction. Dispatching Kafka event for [{}] immediately.", identifier);
            publishTask.run();
        }
    }

    public void syncEvent(Order order) {
        OrderEventType type = switch (order.getStatus()) {
            case PENDING -> OrderEventType.ORDER_INITIATED;
            case PAID -> OrderEventType.PAYMENT_SUCCESS;
            case SHIPPED -> OrderEventType.ORDER_SHIPPED;
            case DELIVERED -> OrderEventType.ORDER_DELIVERED;
            case CONFIRMED -> OrderEventType.ORDER_CONFIRMED;
            case CANCELLED -> OrderEventType.ORDER_CANCELLED;
            case FAILED -> OrderEventType.PAYMENT_FAILED;
            case REFUNDED -> OrderEventType.ORDER_REFUNDED;
        };
        sendOrderEvent(order, type);
    }
}