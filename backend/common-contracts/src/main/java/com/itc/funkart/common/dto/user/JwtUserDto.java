package com.itc.funkart.common.dto.user;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;


/**
 * Security context representation extracted from a validated JWT.
 *
 * <p>Populated by the API Gateway's JWT filter and forwarded to downstream
 * services as an {@code @AuthenticationPrincipal} in Spring Security.</p>
 *
 * @param id   Subject ID claim ({@code sub}) from the token.
 * @param name Display name claim from the token.
 * @param email Principal email claim from the token.
 * @param role Single role string used for authority mapping (e.g., {@code ROLE_USER}).
 */
@Builder
@Jacksonized
public record JwtUserDto(
        Long id,
        String name,
        String email,
        String role
) {}