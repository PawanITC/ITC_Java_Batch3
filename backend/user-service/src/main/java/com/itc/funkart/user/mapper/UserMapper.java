package com.itc.funkart.user.mapper;

import com.itc.funkart.common.dto.user.UserDto;
import com.itc.funkart.common.dto.user.UserProfileDto;
import com.itc.funkart.user.dto.user.OAuthResponse;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.dto.user.UserAdminSummary;
import com.itc.funkart.user.entity.OAuthAccount;
import com.itc.funkart.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps User entities into API-facing response DTOs.
 * Acts as an anti-corruption layer between persistence and API contracts.
 */
@Component
public class UserMapper {

    /**
     * Converts a {@link User} entity into a public-facing {@link UserDto}.
     */
    public UserDto toDto(User user) {
        if (user == null) return null;

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Converts a User entity into an admin-specific summary.
     * Maps the new isActive field for administrative oversight.
     */
    public UserAdminSummary toAdminSummary(User user) {
        if (user == null) return null;

        return new UserAdminSummary(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }

    /**
     * Wraps a user DTO into a login response with JWT token.
     */
    public SuccessfulLoginResponse toResponse(User user, String token) {
        return SuccessfulLoginResponse.builder()
                .user(toDto(user))
                .token(token)
                .build();
    }

    /**
     * Wraps a user DTO into an OAuth login response with JWT token.
     */
    public OAuthResponse toOAuthResponse(User user, String token) {
        return OAuthResponse.builder()
                .user(toDto(user))
                .token(token)
                .build();
    }

    /**
     * Converts a User entity into a full profile representation.
     */
    public UserProfileDto toProfile(User user) {
        if (user == null) return null;

        return UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private List<String> extractProviders(User user) {
        if (user == null || user.getOauthAccounts() == null || user.getOauthAccounts().isEmpty()) {
            return List.of();
        }

        return user.getOauthAccounts()
                .stream()
                .map(OAuthAccount::getProvider)
                .distinct()
                .toList();
    }
}