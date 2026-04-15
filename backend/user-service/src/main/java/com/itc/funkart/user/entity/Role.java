package com.itc.funkart.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the access levels and authorities within the Funkart system.
 * <p>
 * These constants are used by Spring Security for Method Security (@PreAuthorize)
 * and URL-based authorization. Each role maps to a {@code SimpleGrantedAuthority}.
 * </p>
 *
 * @author Abbas Gure
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    /**
     * Standard user role.
     * Permissions: View products, manage own profile, place orders, view own history.
     */
    ROLE_USER("USER"),


    /** Content management privileges (Reviews/Ratings). */
    ROLE_MODERATOR("MODERATOR"),

    /**
     * Administrative role with elevated privileges.
     * Permissions: Manage user accounts, update product catalog, view system metrics,
     * and access administrative dashboards.
     */
    ROLE_ADMIN("ADMIN");

    /**
     * The raw name of the role without the "ROLE_" prefix.
     * Useful for logic that requires the clean name (e.g., specific logging or 3rd party integrations).
     */
    private final String shortName;
}