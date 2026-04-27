package com.itc.funkart.product_service.constants;

/**
 * <h2>KafkaConstants</h2>
 * <p>
 * Centralized registry for Kafka topic names and messaging identifiers.
 * </p>
 * <p>
 * This class ensures that all Producers and Consumers within the Product Service
 * use synchronized topic strings, preventing "Lost Event" bugs caused by typos.
 * </p>
 *
 * <b>Note:</b> These values must be strictly aligned with the topic names defined
 * in the Order and Inventory services to maintain system-wide consistency.
 */
public final class KafkaConstants {

    /**
     * Topic for internal product updates, such as catalog changes or restocks.
     * <p>Used primarily by {@code ProductProducer} and {@code ProductConsumer}.</p>
     */
    public static final String TOPIC_PRODUCTS = "products";

    /**
     * Topic for order-related events.
     * <p>Used to broadcast order creation events to downstream services like
     * Inventory for stock reservation.</p>
     */
    public static final String TOPIC_ORDERS = "order-topic";

    /**
     * Private constructor to prevent instantiation of a constant utility class.
     */
    private KafkaConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}