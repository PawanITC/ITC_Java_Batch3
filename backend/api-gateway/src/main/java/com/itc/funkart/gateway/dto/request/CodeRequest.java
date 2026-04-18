package com.itc.funkart.gateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * <h2>OAuth Authorization Code Wrapper</h2>
 * <p>Sent to the User-Service to exchange a GitHub temporary 'code'
 * for a full Funkart system JWT. This is a single-purpose DTO.</p>
 * * @param code The temporary string provided by GitHub's redirect callback.
 */
@Builder
public record CodeRequest(
        @NotBlank(message = "OAuth code cannot be empty")
        String code
) {}