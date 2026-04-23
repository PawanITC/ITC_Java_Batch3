package com.itc.funkart.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Event published to Kafka upon successful creation of a new user account.
 *
 * @param userId    The newly generated unique identifier.
 * @param email     The registered email address.
 * @param name      The display name provided by the user.
 * @param role      The assigned role (e.g., "ROLE_USER").
 * @param timestamp Epoch milliseconds of the registration.
 */
@Builder
public record UserSignupEvent(
        @JsonProperty("user_id") Long userId,
        String email,
        String name,
        String role,
        Long timestamp
) {
}