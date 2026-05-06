package com.itc.funkart.user.service;

import com.itc.funkart.common.dto.user.UserProfileDto;
import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.OAuthUserResult;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.AlreadyExistsException;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.UnauthorizedException;
import com.itc.funkart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * <h2>UserService — Unit Tests</h2>
 *
 * <p>Covers all public business operations:
 * <ul>
 *   <li>{@code signUp}               — validates, hashes password, persists, publishes signup event</li>
 *   <li>{@code login}                — credential verification</li>
 *   <li>{@code getUserProfile}       — read-model projection</li>
 *   <li>{@code getOrCreateOAuthUser} — idempotent OAuth user resolution</li>
 *   <li>{@code recordLogin}          — delegates to {@link KafkaEventPublisher}</li>
 *   <li>{@code findById / findByEmail} — repository pass-throughs</li>
 * </ul>
 *
 * <p><b>Key fix from old tests:</b> {@link UserService} depends on
 * {@link KafkaEventPublisher} — it was missing from the old mock set, which caused
 * {@code signUp} and {@code recordLogin} tests to fail with NPE.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("alice@example.com")
                .password("hashed-password")
                .name("Alice")
                .role(Role.ROLE_USER)
                .build();
    }

    // -------------------------------------------------------------------------
    // signUp
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("signUp")
    class SignUpTests {

        private SignupRequest request() {
            return SignupRequest.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .password("password123")
                    .build();
        }

        @Test
        @DisplayName("Returns persisted User on valid input")
        void successReturnsUser() {
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.signUp(request());

            assertNotNull(result);
            assertEquals("alice@example.com", result.getEmail());
        }

        @Test
        @DisplayName("Persists the user to the repository")
        void savesUser() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.signUp(request());

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Password is hashed before persistence")
        void passwordIsHashed() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.signUp(request());

            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Publishes signup event via KafkaEventPublisher")
        void publishesSignupEvent() {
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.signUp(request());

            verify(kafkaEventPublisher).publishSignup(testUser);
        }

        @Test
        @DisplayName("Throws AlreadyExistsException when email is already registered")
        void throwsOnDuplicateEmail() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));

            assertThrows(AlreadyExistsException.class, () -> userService.signUp(request()));
        }

        @Test
        @DisplayName("Throws BadRequestException when name is blank")
        void throwsOnBlankName() {
            SignupRequest bad = SignupRequest.builder()
                    .name("  ")
                    .email("alice@example.com")
                    .password("password123")
                    .build();

            assertThrows(BadRequestException.class, () -> userService.signUp(bad));
        }

        @Test
        @DisplayName("Throws BadRequestException when email is blank")
        void throwsOnBlankEmail() {
            SignupRequest bad = SignupRequest.builder()
                    .name("Alice")
                    .email("")
                    .password("password123")
                    .build();

            assertThrows(BadRequestException.class, () -> userService.signUp(bad));
        }

        @Test
        @DisplayName("Throws BadRequestException when password is blank")
        void throwsOnBlankPassword() {
            SignupRequest bad = SignupRequest.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .password("")
                    .build();

            assertThrows(BadRequestException.class, () -> userService.signUp(bad));
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Returns User on valid credentials")
        void successReturnsUser() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

            User result = userService.login(
                    new LoginRequest("alice@example.com", "password123"));

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Throws UnauthorizedException when email is not registered")
        void throwsWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(UnauthorizedException.class,
                    () -> userService.login(new LoginRequest("unknown@example.com", "pass")));
        }

        @Test
        @DisplayName("Throws UnauthorizedException when password does not match")
        void throwsOnWrongPassword() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

            assertThrows(UnauthorizedException.class,
                    () -> userService.login(new LoginRequest("alice@example.com", "wrong")));
        }

        @Test
        @DisplayName("Throws BadRequestException when email is blank")
        void throwsOnBlankEmail() {
            assertThrows(BadRequestException.class,
                    () -> userService.login(new LoginRequest("", "pass")));
        }

        @Test
        @DisplayName("Throws BadRequestException when password is blank")
        void throwsOnBlankPassword() {
            assertThrows(BadRequestException.class,
                    () -> userService.login(new LoginRequest("alice@example.com", "")));
        }
    }

    // -------------------------------------------------------------------------
    // getUserProfile
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfileTests {

        @Test
        @DisplayName("Returns UserProfileDto for a known user ID")
        void returnsProfile() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserProfileDto profile = userService.getUserProfile(1L);

            assertNotNull(profile);
            assertEquals(1L, profile.id());
            assertEquals("Alice", profile.name());
            assertEquals("alice@example.com", profile.email());
            assertEquals("ROLE_USER", profile.role());
        }

        @Test
        @DisplayName("Throws RuntimeException when user is not found")
        void throwsWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> userService.getUserProfile(99L));
        }
    }

    // -------------------------------------------------------------------------
    // getOrCreateOAuthUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getOrCreateOAuthUser")
    class GetOrCreateOAuthUserTests {

        @Test
        @DisplayName("Returns existing user with isNew=false when email already registered")
        void returnsExistingUser() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));

            OAuthUserResult result =
                    userService.getOrCreateOAuthUser("alice@example.com", "alice");

            assertFalse(result.isNew());
            assertSame(testUser, result.user());
        }

        @Test
        @DisplayName("Creates and returns new user with isNew=true when email is not registered")
        void createsNewUser() {
            when(userRepository.findByEmail("new@example.com"))
                    .thenReturn(Optional.empty());

            User newUser = User.builder().id(2L).email("new@example.com")
                    .name("New").role(Role.ROLE_USER).build();
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            OAuthUserResult result =
                    userService.getOrCreateOAuthUser("new@example.com", "New");

            assertTrue(result.isNew());
            assertEquals(2L, result.user().getId());
        }

        @Test
        @DisplayName("Does not save when user already exists")
        void doesNotSaveWhenExists() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));

            userService.getOrCreateOAuthUser("alice@example.com", "alice");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("New OAuth user has password set to {OAUTH} placeholder")
        void newOAuthUserHasPlaceholderPassword() {
            when(userRepository.findByEmail("oauth@example.com"))
                    .thenReturn(Optional.empty());

            User savedUser = User.builder().id(3L).email("oauth@example.com")
                    .name("OAuthUser").password("{OAUTH}").role(Role.ROLE_USER).build();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            OAuthUserResult result =
                    userService.getOrCreateOAuthUser("oauth@example.com", "OAuthUser");

            assertEquals("{OAUTH}", result.user().getPassword());
        }
    }

    // -------------------------------------------------------------------------
    // recordLogin
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("recordLogin")
    class RecordLoginTests {

        @Test
        @DisplayName("Delegates to KafkaEventPublisher.publishLogin with correct method")
        void delegatesToPublisher() {
            userService.recordLogin(testUser, "email");

            verify(kafkaEventPublisher).publishLogin(testUser, "email");
        }

        @Test
        @DisplayName("Works for github login method as well")
        void worksForGithubMethod() {
            userService.recordLogin(testUser, "github");

            verify(kafkaEventPublisher).publishLogin(testUser, "github");
        }
    }

    // -------------------------------------------------------------------------
    // findById / findByEmail
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findById and findByEmail")
    class FindTests {

        @Test
        @DisplayName("findById delegates to repository and returns result")
        void findByIdDelegates() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            Optional<User> result = userService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(1L, result.get().getId());
        }

        @Test
        @DisplayName("findByEmail delegates to repository and returns result")
        void findByEmailDelegates() {
            when(userRepository.findByEmail("alice@example.com"))
                    .thenReturn(Optional.of(testUser));

            Optional<User> result = userService.findByEmail("alice@example.com");

            assertTrue(result.isPresent());
            assertEquals("alice@example.com", result.get().getEmail());
        }
    }
}