package com.itc.funkart.user.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for initiating an OAuth2 handshake.
 * * @param code The temporary authorization code granted by the OAuth provider
 * (e.g., GitHub) after the user approves access.
 */
public record OAuthRequest(
        @NotBlank(message = "Authorization code cannot be empty")
        String code
) {}