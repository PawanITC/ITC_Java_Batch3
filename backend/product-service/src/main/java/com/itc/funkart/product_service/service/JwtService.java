package com.itc.funkart.product_service.service;

import io.jsonwebtoken.Claims;

/**
 * Service interface for handling JSON Web Token (JWT) operations.
 * <p>
 * This service is the security backbone for the Product Service, responsible for
 * verifying the identity and authorities of incoming requests. It ensures all
 * tokens are cryptographically signed by the FunKart Auth Authority.
 * </p>
 */
public interface JwtService {

    /**
     * Parses and validates a raw JWT string.
     * <p>
     * This method performs a signature check and verifies that the 'iss' (Issuer)
     * matches the expected system authority.
     * </p>
     *
     * @param token The raw Bearer or Cookie token string.
     * @return {@link Claims} object containing the user identity and assigned roles.
     * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or tampered with.
     */
    Claims parseJwtToken(String token);
}