package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;

    /**
     * Fast path: returns authenticated user identity from SecurityContext.
     */
    public UserPrincipalDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipalDto principal)) {
            throw new UnauthorizedException("No authenticated user");
        }

        return principal;
    }

    /**
     * Explicit DB rehydration (NOT fallback, intentional enrichment).
     */
    public User getCurrentUserFromDb() {
        Long userId = getCurrentUser().userId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found in DB"));
    }
}