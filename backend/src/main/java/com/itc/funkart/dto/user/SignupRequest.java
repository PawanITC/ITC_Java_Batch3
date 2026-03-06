package com.itc.funkart.dto.user;

/**
 * A dto of the signup request object
 */
public record SignupRequest(
        String email,
        String password,
        String name
) {}
