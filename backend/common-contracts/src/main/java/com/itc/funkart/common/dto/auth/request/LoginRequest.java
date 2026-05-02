package com.itc.funkart.common.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;


/**
 * Credentials for standard email/password authentication.
 */
@Builder
@Jacksonized
public record LoginRequest(
        /* User's registered email. */
        @NotBlank @Email String email,
        /*  User's secret password. */
        @NotBlank String password
) {}