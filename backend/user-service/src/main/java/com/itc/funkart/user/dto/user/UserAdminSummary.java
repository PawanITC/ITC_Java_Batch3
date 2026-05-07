package com.itc.funkart.user.dto.user;

public record UserAdminSummary(
        Long id,
        String name,
        String email,
        String role,
        boolean isActive
) {
}
