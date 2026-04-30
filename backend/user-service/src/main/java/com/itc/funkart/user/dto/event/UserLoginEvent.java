package com.itc.funkart.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Event published to Kafka whenever a user successfully authenticates.
 *
 * @param userId      The unique identifier of the user.
 * @param email       The email address used for login.
 * @param loginMethod The strategy used (e.g., "email", "GitHub").
 * @param role        The user's current role.
 * @param timestamp   Epoch milliseconds of the login occurrence.
 */
@Builder
public record UserLoginEvent(
        @JsonProperty("user_id") Long userId,
        String email,
        @JsonProperty("login_method") String loginMethod,
        String role,
        Long timestamp
) {
}