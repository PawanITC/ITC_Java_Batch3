package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * Generic data transfer object for user profile information.
 * Suitable for internal service-to-service communication.
 * * @param id    The user's unique identifier.
 * @param name  The user's display name.
 * @param email The user's email address.
 */
@Builder
public record UserDto(
        Long id,
        String name,
        String email
) {}