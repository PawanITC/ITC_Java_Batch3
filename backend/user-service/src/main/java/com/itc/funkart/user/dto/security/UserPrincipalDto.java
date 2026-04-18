package com.itc.funkart.user.dto.security;

import lombok.Builder;

/**
 * <h2>User Principal Data Transfer Object</h2>
 *
 * <p>
 * Canonical representation of an authenticated user within the system.
 * This object is used across multiple layers:
 * </p>
 *
 * <ul>
 *   <li><b>Security Context:</b> Represents the authenticated principal after JWT validation.</li>
 *   <li><b>API Layer:</b> Returned to clients as part of authentication responses.</li>
 *   <li><b>OAuth Flow:</b> Used as the standardized user identity payload in OAuth responses.</li>
 * </ul>
 *
 * <p>
 * This DTO replaces both {@code JwtUserDto} and {@code UserDto} to ensure
 * a single source of truth for user identity across the system.
 * </p>
 *
 * <p>
 * <b>Design Principle:</b> Separation of concerns is maintained by ensuring
 * this class does NOT contain security credentials (e.g., passwords or tokens),
 * only identity and authorization metadata.
 * </p>
 *
 * @param userId    The unique database identifier of the user.
 * @param name  The display name of the user.
 * @param email The authenticated email address of the user.
 * @param role  The security role assigned to the user (e.g., ROLE_USER, ROLE_ADMIN).
 */
@Builder
public record UserPrincipalDto(
        Long userId,
        String name,
        String email,
        String role
) {}