package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * The final payload returned to the client upon successful authentication.
 * <p>
 * This record aggregates the user's profile details (via {@link UserDto})
 * with the generated JWT access token required for subsequent authorized requests.
 * </p>
 *
 * @param user  The {@link UserDto} containing the user's identity and assigned roles.
 * @param token The signed JWT string used for Bearer authentication.
 */
@Builder
public record SuccessfulLoginResponse(
        UserDto user,
        String token
) {}
