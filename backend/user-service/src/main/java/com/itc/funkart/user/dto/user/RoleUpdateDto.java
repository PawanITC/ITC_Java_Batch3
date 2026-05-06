package com.itc.funkart.user.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleUpdateDto(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^ROLE_(ADMIN|USER|MODERATOR)$", message = "Invalid role type")
        String role
) {
}
