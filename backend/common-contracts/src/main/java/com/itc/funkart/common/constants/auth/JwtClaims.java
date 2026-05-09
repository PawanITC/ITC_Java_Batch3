package com.itc.funkart.common.constants.auth;

/**
 * <p>Central definition of JWT claims used across the FunKart E-commerce system.</p>
 *
 * <p>This class ensures consistency between the <b>User Service</b> (Token Issuer)
 * and the <b>API Gateway</b> (Token Validator). Any custom claims added to the
 * system should be defined here first.</p>
 *
 * @since 0.0.1-SNAPSHOT
 */
public final class JwtClaims {

    /**
     * The unique identifier for the authentication authority.
     */
    public static final String ISSUER = "funkart-auth-authority";

    /**
     * Standard JWT 'sub' claim for the user identifier.
     */
    public static final String SUBJECT = "sub";

    /**
     * Custom claim for the user's full name.
     */
    public static final String NAME = "name";

    /**
     * Custom claim for the user's email address.
     */
    public static final String EMAIL = "email";

    /**
     * Custom claim for the user's assigned {@code UserRole}.
     */
    public static final String ROLE = "role";

    private JwtClaims() {
        // Prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}