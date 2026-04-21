package com.itc.funkart.user.auth.jwt;

/**
 * Central definition of JWT claims used across the system.
 * Ensures consistency between token generation and parsing.
 */
public final class JwtClaims {

    private JwtClaims() {}

    // The Authority string
    public static final String ISSUER = "funkart-auth-authority";

    // Standard & Custom Claims
    public static final String SUBJECT = "sub";
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String ROLE = "role";
}