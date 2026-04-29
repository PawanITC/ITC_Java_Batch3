package com.itc.funkart.payment.dto.jwt;

import lombok.Builder;

/**
 * <h2>JWT User Identity Record</h2>
 * <p>
 * This record represents the "Hydrated" user identity extracted from JWT claims.
 * It serves as the immutable <b>Principal</b> within the Security Context.
 * </p>
 * <p>
 * <b>DESIGN NOTES:</b>
 * <ol>
 * <li><b>Security Context:</b> Populated by the {@code JwtWebFilter} and passed into the
 * Service layer to ensure {@code userId} is never trust-based from a raw request body.</li>
 * <li><b>Auditability:</b> Contains email and name to populate Stripe metadata
 * for easier customer support and personalized logging.</li>
 * </ol>
 * </p>
 *
 * @param id    Internal FunKart User ID (Subject in JWT).
 * @param name  Full name for Stripe billing/metadata.
 * @param email User email for receipt delivery.
 * @param role  Security role (e.g., "ROLE_USER") for internal authorization.
 */
@Builder
public record JwtUserDto(
        Long id,
        String name,
        String email,
        String role
) {
    /**
     * Helper to verify if the provided ID matches the authenticated user.
     * <p>
     * This is the primary defense against <b>Insecure Direct Object Reference (IDOR)</b>
     * attacks, ensuring users can only interact with their own payment resources.
     * </p>
     *
     * @param otherId The ID to compare against (usually from a database record).
     * @return {@code true} if the IDs match and are not null.
     */
    public boolean matches(Long otherId) {
        return this.id != null && this.id.equals(otherId);
    }
}