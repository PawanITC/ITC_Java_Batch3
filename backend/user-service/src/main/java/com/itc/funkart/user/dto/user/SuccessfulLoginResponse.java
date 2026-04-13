package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * The final payload returned to the client upon successful authentication.
 * Aggregates user profile details with the generated JWT access token.
 * * @param id    The unique identifier for the user.
 *
 * @param email The registered email address.
 * @param name  The display name.
 * @param token The signed JWT string for subsequent authorized requests.
 */
@Builder
public record SuccessfulLoginResponse(
        Long id,
        String email,
        String name,
        String token
) {}
