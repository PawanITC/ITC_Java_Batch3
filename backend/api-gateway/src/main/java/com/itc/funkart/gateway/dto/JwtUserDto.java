package com.itc.funkart.gateway.dto;

/**
 * Represents the claims extracted from a JWT token.
 * Used internally by JwtService and JwtWebFilter.
 * NOT returned to frontend.
 */
public record JwtUserDto(
        Long id,
        String name,
        String email
) {}