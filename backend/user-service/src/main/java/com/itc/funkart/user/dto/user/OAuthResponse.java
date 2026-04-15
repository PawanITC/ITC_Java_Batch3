package com.itc.funkart.user.dto.user;

import lombok.Builder;

/**
 * Specialized response containing the user profile and JWT token
 * generated after successful OAuth processing with third-party providers.
 *
 * @param user  The {@link UserDto} containing the user's identity and assigned roles.
 * @param token The bearer token used for subsequent authorized requests.
 */
@Builder
public record OAuthResponse(
        UserDto user,
        String token
) {}