package com.itc.funkart.common.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

/**
 * Exchange wrapper for GitHub OAuth temporary codes.
 */
@Builder
@Jacksonized
public record CodeRequest(
        /*
         *  The temporary 'code' string returned by GitHub redirect.
         *  */
        @NotBlank String code
) {
}