package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.BadRequestException;
import com.itc.funkart.user.exceptions.NotFoundException;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user-related operations including authentication,
 * registration, and profile retrieval.
 * <p>
 * This controller serves as the entry point for the User Microservice,
 * coordinating between domain services and publishing events to Kafka.
 * </p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final GithubOAuthService githubOAuthService;
    private final JwtService jwtService;
    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Exchanges a GitHub authorization code for a local JWT.
     * * @param body A map containing the "code" provided by GitHub's OAuth callback.
     * @return A {@link ResponseEntity} containing the user profile and a JWT.
     * @throws com.itc.funkart.user.exceptions.OAuthException if the handshake fails.
     */
    @PostMapping("/oauth/github")
    public ResponseEntity<ApiResponse<OAuthResponse>> oauthGithub(@Valid @RequestBody OAuthRequest request) {

        // Process handshake and profile retrieval
        User user = githubOAuthService.processCode(request.code());

        // Generate JWT for the authenticated user
        String userJwt = jwtService.generateJwtToken(new JwtUserDto(
                user.getId(), user.getName(), user.getEmail(), user.getRole().name()));

        // Broadcast login event asynchronously
        kafkaEventPublisher.publishUserLoginEvent(UserLoginEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .loginMethod("github")
                .role(user.getRole().name())
                .timestamp(System.currentTimeMillis())
                .build()
        );

        OAuthResponse oauthresponse = userMapper.toOAuthResponse(user, userJwt);
        return ResponseEntity.ok(new ApiResponse<>(oauthresponse, "GitHub login successful"));
    }

    /**
     * Registers a new user and returns an initial access token.
     * * @param signupRequest Validated DTO containing user registration details.
     * @return A {@link ResponseEntity} with the created user data and a 201 status.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {

        User user = userService.signUp(signupRequest);

        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name())
        );

        // Broadcast signup event synchronously (Reliable delivery)
        kafkaEventPublisher.publishUserSignupEvent(
                UserSignupEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name()) // Fixed builder overwrite
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(resp, "Signup successful"));
    }

    /**
     * Authenticates a user via email/password credentials.
     * * @param loginRequest Validated DTO containing login credentials.
     * @return A {@link ResponseEntity} containing user details and a fresh JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        User user = userService.login(loginRequest);

        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail(), user.getRole().name())
        );

        kafkaEventPublisher.publishUserLoginEvent(
                UserLoginEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .loginMethod("email")
                        .role(user.getRole().name())
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);
        return ResponseEntity.ok(new ApiResponse<>(resp, "Login successful"));
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * * @param jwtUserDto The security principal injected from the JWT context.
     * @return A {@link ResponseEntity} containing the user's public profile data.
     * @throws NotFoundException if the user in the token no longer exists in the DB.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal JwtUserDto jwtUserDto) {

        if (jwtUserDto == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User dbUser = userService.findById(jwtUserDto.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserDto resp = userMapper.toDto(dbUser);
        return ResponseEntity.ok(new ApiResponse<>(resp, "User fetched successfully"));
    }
}