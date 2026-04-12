package com.itc.funkart.user.mapper;

import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserMapper}.
 */
class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    @DisplayName("Mapper: Should correctly map User entity to SuccessfulLoginResponse")
    void toResponse_ShouldMapCorrectly() {
        // Given
        User user = User.builder()
                .id(123L)
                .email("mapper@test.com")
                .name("Mapper Test")
                .password("secret_hash") // This should NOT be in the result
                .build();
        String token = "mock-jwt-token";

        // When
        SuccessfulLoginResponse response = userMapper.toResponse(user, token);

        // Then
        assertAll("Mapping Verification",
                () -> assertEquals(user.getId(), response.id()),
                () -> assertEquals(user.getEmail(), response.email()),
                () -> assertEquals(user.getName(), response.name()),
                () -> assertEquals(token, response.token())
        );
    }
}