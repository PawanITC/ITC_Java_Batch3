package com.itc.funkart.user.auth;

import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.Role;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <h2>AuthFacadeService — Unit Tests</h2>
 *
 * <p>Validates the orchestration logic inside {@link AuthFacadeService}.
 * This service is the single entry point for all three authentication flows:
 * <ul>
 *   <li>GitHub OAuth — {@code handleGithubLogin(code)}</li>
 *   <li>Email/password login — {@code login(LoginRequest)}</li>
 *   <li>Email/password signup — {@code signup(SignupRequest)}</li>
 * </ul>
 *
 * <p>All collaborators are mocked. Tests assert that:
 * <ol>
 *   <li>The correct downstream method is called with the correct arguments</li>
 *   <li>The JWT is generated from the principal built off the resolved user</li>
 *   <li>The login event is recorded via {@code UserService.recordLogin}</li>
 *   <li>The correct response DTO is returned to the controller</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AuthFacadeServiceTest {

    @Mock
    private GithubOAuthService githubOAuthService;
    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PrincipalFactory principalFactory;

    @InjectMocks
    private AuthFacadeService authFacadeService;

    /**
     * A reusable domain user returned by mocked service calls.
     */
    private User testUser;

    /**
     * The principal that PrincipalFactory would derive from testUser.
     */
    private UserPrincipalDto testPrincipal;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .role(Role.ROLE_USER)
                .build();

        testPrincipal = UserPrincipalDto.builder()
                .userId(1L)
                .name("Alice")
                .email("alice@example.com")
                .role("ROLE_USER")
                .build();
    }

    // -------------------------------------------------------------------------
    // handleGithubLogin
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("handleGithubLogin")
    class HandleGithubLoginTests {

        @BeforeEach
        void stubOAuth() {
            when(githubOAuthService.processCode("auth-code")).thenReturn(testUser);
            when(principalFactory.create(testUser)).thenReturn(testPrincipal);
            when(jwtService.generateJwtToken(testPrincipal)).thenReturn("github-jwt");
        }

        @Test
        @DisplayName("Calls GithubOAuthService.processCode with the supplied code")
        void delegatesToGithubService() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toOAuthResponse(testUser, "github-jwt"))
                    .thenReturn(new OAuthResponse(userDto, "github-jwt"));

            authFacadeService.handleGithubLogin("auth-code");

            verify(githubOAuthService).processCode("auth-code");
        }

        @Test
        @DisplayName("Records a github login event via UserService.recordLogin")
        void recordsGithubLoginEvent() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toOAuthResponse(testUser, "github-jwt"))
                    .thenReturn(new OAuthResponse(userDto, "github-jwt"));

            authFacadeService.handleGithubLogin("auth-code");

            verify(userService).recordLogin(testUser, "github");
        }

        @Test
        @DisplayName("Generates JWT using the principal derived from the OAuth user")
        void generatesJwtFromPrincipal() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toOAuthResponse(testUser, "github-jwt"))
                    .thenReturn(new OAuthResponse(userDto, "github-jwt"));

            authFacadeService.handleGithubLogin("auth-code");

            verify(principalFactory).create(testUser);
            verify(jwtService).generateJwtToken(testPrincipal);
        }

        @Test
        @DisplayName("Returns OAuthResponse built by UserMapper")
        void returnsMapperResponse() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            OAuthResponse expected = new OAuthResponse(userDto, "github-jwt");
            when(userMapper.toOAuthResponse(testUser, "github-jwt")).thenReturn(expected);

            OAuthResponse result = authFacadeService.handleGithubLogin("auth-code");

            assertSame(expected, result);
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login")
    class LoginTests {

        private LoginRequest loginRequest;

        @BeforeEach
        void stubLogin() {
            loginRequest = LoginRequest.builder()
                    .email("alice@example.com")
                    .password("password123")
                    .build();

            when(userService.login(loginRequest)).thenReturn(testUser);
            when(principalFactory.create(testUser)).thenReturn(testPrincipal);
            when(jwtService.generateJwtToken(testPrincipal)).thenReturn("login-jwt");
        }

        @Test
        @DisplayName("Delegates credential verification to UserService.login")
        void delegatesToUserService() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toResponse(testUser, "login-jwt"))
                    .thenReturn(new SuccessfulLoginResponse(userDto, "login-jwt"));

            authFacadeService.login(loginRequest);

            verify(userService).login(loginRequest);
        }

        @Test
        @DisplayName("Records an email login event")
        void recordsEmailLoginEvent() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toResponse(testUser, "login-jwt"))
                    .thenReturn(new SuccessfulLoginResponse(userDto, "login-jwt"));

            authFacadeService.login(loginRequest);

            verify(userService).recordLogin(testUser, "email");
        }

        @Test
        @DisplayName("Returns SuccessfulLoginResponse built by UserMapper")
        void returnsMapperResponse() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            SuccessfulLoginResponse expected = new SuccessfulLoginResponse(userDto, "login-jwt");
            when(userMapper.toResponse(testUser, "login-jwt")).thenReturn(expected);

            SuccessfulLoginResponse result = authFacadeService.login(loginRequest);

            assertSame(expected, result);
        }

        @Test
        @DisplayName("Token in response matches generated JWT")
        void tokenMatchesGeneratedJwt() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toResponse(testUser, "login-jwt"))
                    .thenReturn(new SuccessfulLoginResponse(userDto, "login-jwt"));

            SuccessfulLoginResponse result = authFacadeService.login(loginRequest);

            assertEquals("login-jwt", result.token());
        }
    }

    // -------------------------------------------------------------------------
    // signup
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("signup")
    class SignupTests {

        private SignupRequest signupRequest;

        @BeforeEach
        void stubSignup() {
            signupRequest = SignupRequest.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .password("password123")
                    .build();

            when(userService.signUp(signupRequest)).thenReturn(testUser);
            when(principalFactory.create(testUser)).thenReturn(testPrincipal);
            when(jwtService.generateJwtToken(testPrincipal)).thenReturn("signup-jwt");
        }

        @Test
        @DisplayName("Delegates account creation to UserService.signUp")
        void delegatesToUserService() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toResponse(testUser, "signup-jwt"))
                    .thenReturn(new SuccessfulLoginResponse(userDto, "signup-jwt"));

            authFacadeService.signup(signupRequest);

            verify(userService).signUp(signupRequest);
        }

        @Test
        @DisplayName("Records an email login event immediately after signup")
        void recordsLoginEventAfterSignup() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            when(userMapper.toResponse(testUser, "signup-jwt"))
                    .thenReturn(new SuccessfulLoginResponse(userDto, "signup-jwt"));

            authFacadeService.signup(signupRequest);

            verify(userService).recordLogin(testUser, "email");
        }

        @Test
        @DisplayName("Returns SuccessfulLoginResponse built by UserMapper")
        void returnsMapperResponse() {
            UserDto userDto = new UserDto(1L, "Alice", "alice@example.com", "ROLE_USER");
            SuccessfulLoginResponse expected = new SuccessfulLoginResponse(userDto, "signup-jwt");
            when(userMapper.toResponse(testUser, "signup-jwt")).thenReturn(expected);

            SuccessfulLoginResponse result = authFacadeService.signup(signupRequest);

            assertSame(expected, result);
        }
    }
}