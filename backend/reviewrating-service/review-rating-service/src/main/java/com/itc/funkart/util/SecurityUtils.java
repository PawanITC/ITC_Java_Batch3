package com.itc.funkart.util;

import com.itc.funkart.common.dto.user.JwtUserDto;
import com.itc.funkart.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof JwtUserDto user)) {
            throw new UnauthorizedException("No valid security token found");
        }

        Long userId = user.id();
        if (userId == null) {
            throw new UnauthorizedException("User ID missing in security context");
        }

        return userId;
    }

    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof JwtUserDto user)) {
            throw new UnauthorizedException("No valid security token found");
        }

        return user.role();
    }
}
