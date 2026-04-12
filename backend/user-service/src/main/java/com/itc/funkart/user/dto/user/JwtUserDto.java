package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * Internal identity carrier representing a validated user within the security context.
 * Used for generating JWT claims and identifying the user in controller methods.
 * * @param id    The unique database identifier of the user.
 *
 * @param name  The user's display name.
 * @param email The user's authenticated email address.
 */
@Builder
public record JwtUserDto(
        Long id,
        String name,
        String email
) {
}