package com.itc.funkart.gateway.dto;

import lombok.Builder;

/**
 * Represents the "Hydrated" user identity extracted from JWT claims.
 * Used by {@code JwtWebFilter} to populate the Reactive Security Context.
 */
@Builder
public record JwtUserDto(
        Long id,
        String name,
        String email,
        String role
) {}