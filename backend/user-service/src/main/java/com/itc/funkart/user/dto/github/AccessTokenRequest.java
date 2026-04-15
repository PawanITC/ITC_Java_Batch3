package com.itc.funkart.user.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Payload sent to GitHub's OAuth endpoint to exchange an authorization code for an access token.
 *
 * @param clientId     The unique Client ID assigned by GitHub.
 * @param clientSecret The secret key generated for the GitHub OAuth App.
 * @param code         The temporary authorization code from the redirect.
 * @param redirectUri  The callback URL for authorization completion.
 */
@Builder
public record AccessTokenRequest(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        String code,
        @JsonProperty("redirect_uri") String redirectUri
) {
}

