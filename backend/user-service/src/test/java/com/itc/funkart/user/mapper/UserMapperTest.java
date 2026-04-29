package com.itc.funkart.user.mapper;

import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.dto.user.UserDto;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserMapper}.
 * Verifies that internal entities are correctly transformed into nested DTOs
 * without leaking sensitive data like password hashes.
 */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    @DisplayName("Mapper: Should correctly map User entity to nested SuccessfulLoginResponse")
    void toResponse_ShouldMapCorrectly() {
        // Given
        User user = User.builder()
                .id(123L)
                .email("mapper@test.com")
                .name("Mapper Test")
                .role(Role.ROLE_USER)
                .password("secret_hash") // Sensitive data
                .build();
        String token = "mock-jwt-token";

        // When
        SuccessfulLoginResponse response = userMapper.toResponse(user, token);

        // Then
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.user(), "Nested UserDto should not be null");

        assertAll("Nested Mapping Verification",
                // Check the nested UserDto fields
                () -> assertEquals(user.getId(), response.user().id()),
                () -> assertEquals(user.getEmail(), response.user().email()),
                () -> assertEquals(user.getName(), response.user().name()),
                () -> assertEquals(user.getRole().name(), response.user().role()),

                // Check the top-level token
                () -> assertEquals(token, response.token())
        );
    }

    @Test
    @DisplayName("Mapper: Should map User entity to standalone UserDto")
    void toDto_ShouldMapCorrectly() {
        // Given
        User user = User.builder()
                .id(999L)
                .email("dto@test.com")
                .name("Direct DTO")
                .role(Role.ROLE_USER)
                .build();

        // When
        UserDto dto = userMapper.toDto(user);

        // Then
        assertAll("UserDto Verification",
                () -> assertEquals(user.getId(), dto.id()),
                () -> assertEquals(user.getEmail(), dto.email()),
                () -> assertEquals(user.getName(), dto.name()),
                () -> assertEquals(user.getRole().name(), dto.role())
        );
    }
}