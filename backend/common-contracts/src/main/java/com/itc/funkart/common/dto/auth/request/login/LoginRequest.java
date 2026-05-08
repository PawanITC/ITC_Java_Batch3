package com.itc.funkart.common.dto.auth.request.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;


/**
 * Credentials for standard email/password authentication.
 *
 * @param email    The user's registered email address. Must be a valid RFC-compliant email.
 * @param password The user's plaintext password. Validated server-side against the BCrypt hash.
 */
@Builder
@Jacksonized
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}