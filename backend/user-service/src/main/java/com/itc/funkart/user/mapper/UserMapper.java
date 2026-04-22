package com.itc.funkart.user.mapper;

import com.itc.funkart.user.dto.user.OAuthResponse;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.dto.user.UserDto;
import com.itc.funkart.user.dto.user.UserProfileDto;
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
     *
     * <p>Includes derived OAuth provider information extracted from linked accounts.</p>
     *
     * @param user persisted user entity
     * @return API-safe user representation, or null if input is null
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
     * Wraps a user DTO into a login response with JWT token.
     *
     * @param user  authenticated user entity
     * @param token generated JWT token
     * @return login response payload
     */
    public SuccessfulLoginResponse toResponse(User user, String token) {
        return SuccessfulLoginResponse.builder()
                .user(toDto(user))
                .token(token)
                .build();
    }

    /**
     * Wraps a user DTO into an OAuth login response with JWT token.
     *
     * @param user  authenticated/created OAuth user entity
     * @param token generated JWT token
     * @return OAuth response payload
     */
    public OAuthResponse toOAuthResponse(User user, String token) {
        return OAuthResponse.builder()
                .user(toDto(user))
                .token(token)
                .build();
    }

    /**
     * Extracts OAuth provider names from linked accounts.
     *
     * @param user user entity
     * @return list of provider names or empty list if none exist
     */
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


    /**
     * Converts a User entity into a full profile representation including OAuth providers.
     *
     * @param user persisted user entity
     * @return extended profile DTO for account-related endpoints
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
}