package com.itc.funkart.review.auth;

import com.itc.funkart.common.constants.auth.JwtClaims;
import com.itc.funkart.common.dto.security.UserPrincipalDto;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PrincipalFactory {

    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_MODERATOR", "ROLE_ADMIN");

    public UserPrincipalDto fromClaims(Claims claims) {
        if (claims == null) return null;

        long userId;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }

        String role = normalizeRole(claims.get(JwtClaims.ROLE, String.class));
        String name = claims.get(JwtClaims.NAME, String.class);
        String email = claims.get(JwtClaims.EMAIL, String.class);

        if (name == null || email == null) return null;

        return UserPrincipalDto.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .role(role)
                .build();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return DEFAULT_ROLE;
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;
        return ALLOWED_ROLES.contains(role) ? role : DEFAULT_ROLE;
    }
}
