package com.itc.funkart.user.mapper;

import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper utility responsible for transforming {@link User} entities into
 * secure, client-facing Data Transfer Objects.
 * This abstraction ensures sensitive fields like hashed passwords are never exposed.
 */
@Component
public class UserMapper {

    /**
     * Maps a User entity and a fresh JWT into a {@link SuccessfulLoginResponse} record.
     * * @param user  The persistent user entity.
     * @param token The generated JWT bearer token.
     * @return A sanitized login response.
     */
    public SuccessfulLoginResponse toResponse(User user, String token) {
        // Using the Record constructor directly
        return new SuccessfulLoginResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                token
        );
    }
}