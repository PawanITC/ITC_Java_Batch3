package com.itc.funkart.user.dto.github;

import lombok.Builder;

/**
 * Response received from GitHub containing the OAuth access token.
 * * @param access_token The token used to authenticate requests to the GitHub API.
 *
 * @param scope      The permissions granted by the user (e.g., 'user:email').
 * @param token_type The type of token returned, typically {@code "bearer"}.
 */
@Builder
public record AccessTokenResponse(
        String access_token,
        String scope,
        String token_type
) {
}