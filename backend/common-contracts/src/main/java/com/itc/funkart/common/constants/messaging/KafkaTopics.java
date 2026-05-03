package com.itc.funkart.common.constants.messaging;

/**
 * Global registry of Kafka topic names to ensure consistency
 * across the Funkart microservice ecosystem.
 */
public final class KafkaTopics {

    // --- AUTH & USER DOMAIN ---
    public static final String AUTH_LOGIN = "auth.events.login.v1";
    public static final String USER_SIGNUP = "users.events.signup.v1";
    public static final String USER_UPDATED = "users.events.updated.v1";

    // --- PRODUCT & INVENTORY DOMAIN ---
    public static final String PRODUCTS = "products.events.v1";
    public static final String PRODUCT_METADATA = "products.events.metadata.v1";
    public static final String PRODUCT_INVENTORY = "products.events.inventory.v1";

    // --- ORDER DOMAIN ---
    public static final String ORDERS = "orders.events.v1";
    public static final String ORDERS_DLQ = "orders.events.v1.dlq";

    // --- PAYMENT DOMAIN ---
    public static final String PAYMENT_PROCESS = "payments.events.v1";
    public static final String PAYMENT_DLQ = "payments.events.v1.dlq";

    // --- NOTIFICATION DOMAIN ---
    public static final String NOTIFICATIONS = "notifications.events.v1";

    private KafkaTopics() {
        // Prevent instantiation in the JVM Heap
        throw new UnsupportedOperationException("Constant utility class");
    }
}