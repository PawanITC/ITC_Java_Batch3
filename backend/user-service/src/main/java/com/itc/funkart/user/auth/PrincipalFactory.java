package com.itc.funkart.user.auth;

import com.itc.funkart.user.auth.jwt.JwtClaims;
import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Central identity mapping boundary:
 * DB → JWT → SecurityContext
 */
@Component
public class PrincipalFactory {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_USER",
            "ROLE_MODERATOR",
            "ROLE_ADMIN"
    );

    // ---------------- DOMAIN → PRINCIPAL ----------------

    public UserPrincipalDto create(User user) {
        if (user == null) return null;

        String role = normalizeAndValidateRole(
                user.getRole() != null ? user.getRole().name() : DEFAULT_ROLE
        );

        return UserPrincipalDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(role)
                .build();
    }

    // ---------------- JWT → PRINCIPAL ----------------

    public UserPrincipalDto fromClaims(Claims claims) {
        if (claims == null) {
            throw new JwtAuthenticationException("Missing JWT claims");
        }

        long userId = parseUserId(claims.getSubject());

        String role = normalizeAndValidateRole(
                safeString(claims.get(JwtClaims.ROLE, String.class))
        );

        String name = safeString(claims.get(JwtClaims.NAME, String.class));
        String email = safeString(claims.get(JwtClaims.EMAIL, String.class));

        if (name == null || email == null) {
            throw new JwtAuthenticationException("Missing identity claims in JWT");
        }

        return UserPrincipalDto.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .role(role)
                .build();
    }

    // ---------------- CORE SAFETY LAYER ----------------

    private long parseUserId(String subject) {
        if (subject == null || !subject.matches("\\d+")) {
            throw new JwtAuthenticationException("Invalid JWT subject format");
        }

        try {
            return Long.parseLong(subject);
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Invalid JWT subject parsing");
        }
    }

    private String safeString(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * Single role enforcement gate:
     * - default fallback
     * - normalization
     * - whitelist enforcement
     */
    private String normalizeAndValidateRole(String role) {

        String normalized = (role == null || role.isBlank())
                ? DEFAULT_ROLE
                : role;

        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }

        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new JwtAuthenticationException(
                    "Unauthorized role in token: " + normalized
            );
        }

        return normalized;
    }
}