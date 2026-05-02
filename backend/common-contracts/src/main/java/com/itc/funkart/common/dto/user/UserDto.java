package com.itc.funkart.common.dto.user;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

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
@Jacksonized
public record UserDto(
    /* Database primary key. */
    Long id,
    /* User's full name. */
    String name,
    /* User's email. */
    String email,
    /* Assigned security role (e.g., ROLE_USER). */
    String role
) {}