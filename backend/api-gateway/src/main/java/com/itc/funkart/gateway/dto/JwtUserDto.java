package com.itc.funkart.gateway.dto;

import lombok.Builder;

/**
 * <h2>JWT User Identity Record</h2>
 * <p>
 * This record represents the "Hydrated" user identity extracted from JWT claims.
 * It serves as the immutable <b>Principal</b> within the Reactive Security Context
 * and is used to propagate identity to downstream microservices.
 * </p>
 * @param id    The unique database identifier for the user.
 * @param name  The display name or full name of the user.
 * @param email The authenticated email address.
 * @param role  The security role (e.g., "ROLE_USER" or "ROLE_ADMIN").
 */
@Builder
public record JwtUserDto(
        Long id,
        String name,
        String email,
        String role
) {}