package com.itc.funkart.common.dto.event.signup;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Event published to Kafka upon successful creation of a new user account.
 * <p>
 * Contract for User Signup.
 * Moved to common to allow Email and Analytics services to process new users.
 *
 * @param userId    The newly generated unique identifier.
 * @param email     The registered email address.
 * @param name      The display name provided by the user.
 * @param role      The assigned role (e.g., "ROLE_USER").
 * @param timestamp Epoch milliseconds of the registration.
 */
public record UserSignupEvent(
        @JsonProperty("user_id") Long userId,
        String email,
        String name,
        String role,
        Long timestamp
) {
    /**
     * Convenience factory — automatically captures the current epoch timestamp.
     *
     * @param userId The newly generated user ID from the database.
     * @param email  The registered email address.
     * @param name   The display name provided during registration.
     * @param role   The assigned role (e.g., {@code "ROLE_USER"}).
     * @return A fully populated {@link UserSignupEvent}.
     */
    public static UserSignupEvent of(Long userId, String email, String name, String role) {
        return new UserSignupEvent(
                userId,
                email,
                name,
                role,
                Instant.now().toEpochMilli()
        );
    }
}