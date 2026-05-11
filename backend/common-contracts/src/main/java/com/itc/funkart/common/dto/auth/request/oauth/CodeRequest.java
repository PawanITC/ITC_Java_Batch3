package com.itc.funkart.common.dto.auth.request.oauth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Exchange wrapper for GitHub OAuth temporary codes.
 *
 * @param code The temporary one-time code returned by GitHub's redirect URI.
 *             This code is exchanged server-side for an access token.
 */
@Builder
@Jacksonized
public record CodeRequest(
        @NotBlank String code
) {
}