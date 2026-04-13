package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.OAuthResponse;
import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.NotFoundException;
import com.itc.funkart.user.mapper.UserMapper;
import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user-related operations including authentication,
 * registration, and profile retrieval.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final GithubOAuthService githubOAuthService;
    private final JwtService jwtService;
    private final KafkaEventPublisher kafkaEventPublisher;

    public UserController(UserService userService,
                          GithubOAuthService githubOAuthService,
                          UserMapper userMapper,
                          JwtService jwtService,
                          KafkaEventPublisher kafkaEventPublisher) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.githubOAuthService = githubOAuthService;
        this.jwtService = jwtService;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    /**
     * GitHub OAuth endpoint.
     * Exchanges an auth code for a user profile and returns a JWT.
     */
    @PostMapping("/oauth/github")
    public ResponseEntity<OAuthResponse> oauthGithub(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Rely on GlobalExceptionHandler for error mapping
        User user = githubOAuthService.processCode(code);

        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail())
        );

        kafkaEventPublisher.publishUserLoginEvent(
                UserLoginEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .loginMethod("github")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        return ResponseEntity.ok(new OAuthResponse(jwt));
    }

    /**
     * User signup endpoint.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {

        User user = userService.signUp(signupRequest);

        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail())
        );

        kafkaEventPublisher.publishUserSignupEvent(
                UserSignupEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(resp, "Signup successful"));
    }

    /**
     * User login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        User user = userService.login(loginRequest);

        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail())
        );

        kafkaEventPublisher.publishUserLoginEvent(
                UserLoginEvent.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .loginMethod("email")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);
        return ResponseEntity.ok(new ApiResponse<>(resp, "Login successful"));
    }

    /**
     * Retrieves the current authenticated user's profile based on the JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> getCurrentUser(
            @AuthenticationPrincipal JwtUserDto user) {

        // This check is now safer; if security filter works, user is never null here
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User dbUser = userService.findById(user.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        SuccessfulLoginResponse resp = userMapper.toResponse(dbUser, null);
        return ResponseEntity.ok(new ApiResponse<>(resp, "User fetched successfully"));
    }
}