package com.itc.funkart.common.constants.messaging;

/**
 * Global registry of Kafka topic names to ensure consistency
 * across the Funkart microservice ecosystem.
 */
public final class KafkaTopics {

    // --- USER DOMAIN ---
    public static final String USER_SIGNUP = "users.events.signup.v1";
    public static final String USER_UPDATED = "users.events.updated.v1";
    public static final String AUTH_LOGIN = "auth.events.login.v1";

    // --- PRODUCT DOMAIN ---
    public static final String PRODUCTS = "products.events.v1";

    // --- ORDER DOMAIN ---
    public static final String ORDERS = "orders.events.v1";

    private KafkaTopics() {
        throw new UnsupportedOperationException("Constant utility class");
    }
}