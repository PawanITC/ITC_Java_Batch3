package com.itc.funkart.common.dto.event.login;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event published to Kafka whenever a user successfully authenticates.
 *
 * @param userId      The unique identifier of the user.
 * @param email       The email address used for login.
 * @param loginMethod The strategy used (e.g., "email", "GitHub").
 * @param role        The user's current role.
 * @param timestamp   Epoch milliseconds of the login occurrence.
 */
public record UserLoginEvent(
        @JsonProperty("user_id") Long userId,
        String email,
        @JsonProperty("login_method") String loginMethod,
        String role,
        Long timestamp
) {
    /**
     * Business Factory Method.
     */
    public static UserLoginEvent of(Long userId, String email, String method, String role) {
        return new UserLoginEvent(
                userId,
                email,
                method,
                role,
                Instant.now().toEpochMilli()
        );
    }
}