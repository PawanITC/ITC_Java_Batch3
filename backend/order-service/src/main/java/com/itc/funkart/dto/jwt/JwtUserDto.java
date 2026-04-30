package com.itc.funkart.dto.jwt;

import lombok.Builder;

/**
 * Data Transfer Object representing an authenticated user within the security context.
 * <p>
 * This object is extracted from JWT claims and serves as the 'Principal' for
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}.
 * It allows controllers to access user details without reparsing the token.
 * </p>
 */
@Builder
public record JwtUserDto(
        Long id,
        String name,
        String email,
        String role
) {
}
