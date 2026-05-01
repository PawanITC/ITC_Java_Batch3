package com.itc.funkart.product_service.util;

import com.itc.funkart.product_service.dto.jwt.JwtUserDto;
import com.itc.funkart.product_service.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to handle Security related operations.
 */
public class SecurityUtils {

    /**
     * Extracts the user ID from the JWT stored in the current Thread's SecurityContext.
     *
     * @return Long the unique user ID
     * @throws RuntimeException if the user is not authenticated or the ID is missing
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if the principal is our custom JwtUserDto
        if (auth == null || !(auth.getPrincipal() instanceof JwtUserDto user)) {
            throw new UnauthorizedException("No valid security token found");
        }

        // Use the ID directly from our DTO
        Long userId = user.id();

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID missing in Security Context");
        }

        return userId;
    }
}