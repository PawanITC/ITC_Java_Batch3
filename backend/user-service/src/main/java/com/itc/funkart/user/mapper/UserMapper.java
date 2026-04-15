package com.itc.funkart.user.mapper;

import com.itc.funkart.user.dto.user.OAuthResponse;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.dto.user.UserDto;
import com.itc.funkart.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper utility responsible for transforming {@link User} entities into
 * secure, client-facing Data Transfer Objects.
 */
@Component
public class UserMapper {

    /**
     * Maps a User entity to a standard {@link UserDto}.
     * This is the "base" mapping used across the application.
     */
    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name()) // Mapping the Enum to String
                .build();
    }

    /**
     * Maps a User entity and a JWT into a {@link SuccessfulLoginResponse}.
     * This uses the toDto method to ensure consistency.
     */
    public SuccessfulLoginResponse toResponse(User user, String token) {
        return SuccessfulLoginResponse.builder()
                .user(toDto(user)) // Nesting the DTO
                .token(token)
                .build();
    }

    /**
     * For GitHub/Social Login
     */
    public OAuthResponse toOAuthResponse(User user, String token) {
        return OAuthResponse.builder()
                .user(toDto(user))
                .token(token)
                .build();
    }
}