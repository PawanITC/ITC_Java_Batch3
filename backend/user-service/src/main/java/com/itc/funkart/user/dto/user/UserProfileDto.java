package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * <h2>User Profile Data Transfer Object</h2>
 *
 * <p>
 * Represents an extended view of a user intended for profile-related API responses.
 * Unlike {@link UserDto}, this DTO includes additional relational metadata such as
 * linked OAuth providers.
 * </p>
 *
 * <p>
 * This object is NOT used for authentication or security context.
 * It is strictly a presentation-layer model for user profile endpoints.
 * </p>
 *
 * <h3>Usage</h3>
 * <ul>
 *   <li>/users/me/profile endpoint</li>
 *   <li>User settings pages</li>
 *   <li>Account management UI</li>
 * </ul>
 *
 * @param id    Unique user identifier
 * @param name  Display name of the user
 * @param email Email address of the user
 * @param role  Assigned system role (e.g. ROLE_USER)
 */
@Builder
public record UserProfileDto(
        Long id,
        String name,
        String email,
        String role
) {
}