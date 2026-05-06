package com.itc.funkart.common.constants.messaging;

/**
 * Funkart Kafka Topic Registry.
 *
 * Centralized definition of all Kafka topics used across the platform.
 * Ensures consistent event routing and prevents topic drift.
 *
 * Event Flow:
 * checkout.events.v1 → Order Service
 * orders.events.v1 → downstream consumers
 * payments.events.v1 → order + notification consumers
 */
public final class KafkaTopics {

    // AUTH DOMAIN
    public static final String AUTH_LOGIN = "auth.events.login.v1";
    public static final String USER_SIGNUP = "users.events.signup.v1";
    public static final String USER_UPDATED = "users.events.updated.v1";

    // PRODUCT DOMAIN
    public static final String PRODUCTS = "products.events.v1";
    public static final String PRODUCT_METADATA = "products.events.metadata.v1";
    public static final String PRODUCT_INVENTORY = "products.events.inventory.v1";

    // CHECKOUT DOMAIN
    public static final String CHECKOUT_INITIATED = "checkout.events.v1";

    // ORDER DOMAIN
    public static final String ORDERS = "orders.events.v1";
    public static final String ORDERS_DLQ = "orders.events.v1.dlq";

    // PAYMENT DOMAIN
    public static final String PAYMENTS_EVENTS = "payments.events.v1";
    public static final String PAYMENTS_DLQ = "payments.events.v1.dlq";

    // NOTIFICATION DOMAIN
    public static final String NOTIFICATIONS = "notifications.events.v1";

    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}