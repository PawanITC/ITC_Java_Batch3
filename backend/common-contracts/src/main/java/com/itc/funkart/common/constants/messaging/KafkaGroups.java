package com.itc.funkart.common.constants.messaging;

/**
 * Funkart Kafka Consumer Group Registry.

 * Centralized registry of Kafka consumer group IDs.
 * Ensures consistent consumption semantics and prevents accidental
 * creation of duplicate consumer groups due to naming drift.

 * Versioning Strategy:
 * Consumer groups are versioned (v1 → v2 → v3...) when:
 * - Offsets must be reset due to schema or deserialization changes
 * - Poison-pill messages block processing
 * - Breaking event contract changes occur

 * Incrementing version forces Kafka to treat the service as a new consumer group.
 */
public final class KafkaGroups {

    public static final String ORDER_SERVICE_GROUP = "order-service-group-v2";

    public static final String PAYMENT_SERVICE_GROUP = "payment-service-group-v6";

    public static final String PRODUCT_SERVICE_GROUP = "product-service-group-v1";

    /** Reserved for user-domain event consumption. */
    public static final String USER_SERVICE_GROUP = "user-service-group-v1";

    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-group-v1";

    private KafkaGroups() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}