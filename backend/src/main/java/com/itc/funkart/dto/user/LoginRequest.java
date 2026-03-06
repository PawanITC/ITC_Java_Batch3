package com.itc.funkart.dto.user;

/**
 * A dto of the login request object
 */
public record LoginRequest(
        String email,
        String password
) {}
