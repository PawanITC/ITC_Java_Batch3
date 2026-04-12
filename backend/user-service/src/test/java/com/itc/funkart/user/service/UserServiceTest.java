package com.itc.funkart.user.service;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 * Covers successful flows and edge cases like duplicate registration and invalid credentials.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private SignupRequest signupRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        signupRequest = SignupRequest.builder()
                .email("test@example.com")
                .password("password123")
                .name("Test User")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("hashedPassword")
                .name("Test User")
                .build();
    }

    @Test
    @DisplayName("SignUp: Should successfully register a new user")
    void signUp_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.signUp(signupRequest);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("SignUp: Should throw AlreadyExistsException when email is taken")
    void signUp_DuplicateEmail() {
        when(userRepository.findByEmail(signupRequest.email())).thenReturn(Optional.of(testUser));

        assertThrows(AlreadyExistsException.class, () -> userService.signUp(signupRequest));
    }

    @Test
    @DisplayName("Login: Should return user on valid credentials")
    void login_Success() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        User result = userService.login(loginRequest);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
    }

    @Test
    @DisplayName("Login: Should throw UnauthorizedException on wrong password")
    void login_WrongPassword() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrong");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.login(loginRequest));
    }
}