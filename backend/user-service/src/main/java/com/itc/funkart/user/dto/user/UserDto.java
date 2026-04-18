package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * <h2>User Data Transfer Object</h2>
 *
 * <p>
 * Minimal representation of a user exposed through standard API responses.
 * Designed for lightweight usage in authentication responses and listings.
 * </p>
 *
 * <p>
 * This DTO intentionally excludes relational or sensitive metadata
 * such as OAuth providers or security context information.
 * </p>
 *
 * @param id    Unique identifier of the user
 * @param name  Display name
 * @param email Email address
 * @param role  Assigned system role
 */
@Builder
public record UserDto(
        Long id,
        String name,
        String email,
        String role
) {}