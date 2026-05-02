package com.itc.funkart.common.constants.messaging;

public final class KafkaMessaging {

    // --- TOPIC NAMES ---
    public static final String TOPIC_USER_SIGNUP = "users.events.signup.v1";
    public static final String TOPIC_USER_UPDATED = "users.events.updated.v1";
    public static final String TOPIC_AUTH_LOGIN = "auth.events.login.v1";

    // --- SHARED GROUP IDS (If multiple services act as one logical consumer) ---
    // Note: Usually, services define their own specific group IDs in application.yml
    public static final String GROUP_USER_SERVICE = "user-service-group";

    private KafkaMessaging() {} // Prevent instantiation
}