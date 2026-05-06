package com.itc.funkart.common.dto.user;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;


/**
 * Security context representation extracted from a JWT.
 */
@Builder
@Jacksonized
public record JwtUserDto(
        /* Subject ID from the token. */
        Long id,
        /* Display name. */
        String name,
        /* Principal email. */
        String email,
        /* Single role string for authority mapping. */
        String role
) {}