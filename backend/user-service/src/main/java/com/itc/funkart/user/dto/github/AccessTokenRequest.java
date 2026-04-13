package com.itc.funkart.user.dto.github;

import lombok.Builder;

/**
 * Payload sent to GitHub's OAuth endpoint to exchange an authorization code for an access token.
 * * @param client_id     The unique Client ID assigned by GitHub when the OAuth App was created.
 *
 * @param client_secret The secret key generated for the GitHub OAuth App.
 * @param code          The temporary authorization code received from the GitHub redirect.
 * @param redirect_uri  The callback URL where GitHub sends the user after authorization.
 */
@Builder
public record AccessTokenRequest(
        String client_id,
        String client_secret,
        String code,
        String redirect_uri
) {
}

