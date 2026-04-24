package com.itc.funkart.user.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Response received from GitHub containing the OAuth access token.
 *
 * @param accessToken The token used to authenticate requests to the GitHub API.
 * @param scope       The permissions granted by the user (e.g., 'user:email').
 * @param tokenType   The type of token returned, typically {@code "bearer"}.
 */
@Builder
public record AccessTokenResponse(
        @JsonProperty("access_token") String accessToken,
        String scope,
        @JsonProperty("token_type") String tokenType
) {
}