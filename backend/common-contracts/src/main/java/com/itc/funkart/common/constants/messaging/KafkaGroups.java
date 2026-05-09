package com.itc.funkart.common.constants.messaging;

/**
 * Funkart Kafka Consumer Group Registry.
 * <p>
 * Centralized registry of Kafka consumer group IDs.
 * Ensures consistent consumption semantics and prevents accidental
 * creation of duplicate consumer groups due to naming drift.
 * <p>
 * Versioning Strategy:
 * Consumer groups are versioned (v1 → v2 → v3...) when:
 * - Offsets must be reset due to schema or deserialization changes
 * - Poison-pill messages block processing
 * - Breaking event contract changes occur
 * <p>
 * Incrementing version forces Kafka to treat the service as a new consumer group.
 */
public final class KafkaGroups {

    /**
     * Consumer group for the Order Service. Increment version when offsets must be reset.
     */
    public static final String ORDER_SERVICE_GROUP = "order-service-group-v2";

    /**
     * Consumer group for the Payment Service. Increment version when offsets must be reset.
     */
    public static final String PAYMENT_SERVICE_GROUP = "payment-service-group-v6";

    /**
     * Consumer group for the Product/Inventory Service.
     */
    public static final String PRODUCT_SERVICE_GROUP = "product-service-group-v1";

    /**
     * Consumer group for the User Service (auth and profile events).
     */
    public static final String USER_SERVICE_GROUP = "user-service-group-v1";

    /**
     * Consumer group for the Notification Service (email and SMS delivery).
     */
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-group-v1";

    /**
     * Consumer group for the Rating Aggregator Service (CQRS read side).
     * Consumes review.events.v1 to maintain per-product rating summaries.
     */
    public static final String RATING_AGGREGATOR_SERVICE_GROUP = "rating-aggregator-service-group-v1";

    private KafkaGroups() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}