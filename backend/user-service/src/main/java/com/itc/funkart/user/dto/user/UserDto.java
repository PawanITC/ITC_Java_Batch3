package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * Generic data transfer object for user profile information.
 * Suitable for internal service-to-service communication and profile responses.
 *
 * @param id    The user's unique identifier.
 * @param name  The user's display name.
 * @param email The user's email address.
 * @param role  The user's security role (e.g., ROLE_USER).
 */
@Builder
public record UserDto(
        Long id,
        String name,
        String email,
        String role
) {}