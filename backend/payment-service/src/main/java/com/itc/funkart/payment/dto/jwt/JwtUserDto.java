package com.itc.funkart.payment.dto.jwt;

/**
 * Data Transfer Object representing the authenticated user extracted from a JWT.
 * <p>
 * DESIGN NOTES:
 * 1. <b>Security Context:</b> This record is typically populated by the
 * Security Filter/Interceptor and passed into the Service layer to ensure
 * that 'userId' is never trust-based from a raw request body.
 * 2. <b>Auditability:</b> Contains the email and name to allow for personalized
 * logging or to populate Stripe metadata for easier customer support.
 * </p>
 */
public record JwtUserDto(
        Long id,      // Internal FunKart User ID
        String name,  // Full name for Stripe billing/metadata
        String email  // User email for receipt delivery
) {
    /**
     * Helper to verify if the provided ID matches the authenticated user.
     * Prevents "Insecure Direct Object Reference" (IDOR) attacks.
     */
    public boolean matches(Long otherId) {
        return this.id != null && this.id.equals(otherId);
    }
}