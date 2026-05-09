package com.itc.funkart.common.enums.user;

/**
 * <h2>System Authorities</h2>
 * Defines the access levels available across the Funkart ecosystem.
 * Used by Security Filters to perform Role-Based Access Control (RBAC).
 */
public enum UserRole {
    /**
     * Standard customer with access to browsing and purchasing.
     */
    ROLE_USER,

    /**
     * Administrative user with access to inventory and user management.
     */
    ROLE_ADMIN,

    /**
     * Support staff with limited access to order management.
     */
    ROLE_MODERATOR
}