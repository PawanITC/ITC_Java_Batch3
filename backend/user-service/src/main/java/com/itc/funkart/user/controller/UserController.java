package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.OAuthResponse;
import com.itc.funkart.user.dto.event.UserLoginEvent;
import com.itc.funkart.user.dto.event.UserSignupEvent;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.NotFoundException;
import com.itc.funkart.user.mapper.user.UserMapper;
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

@RestController
@RequestMapping("${api.version}/users")
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
     * GitHub OAuth endpoint
     * Processes OAuth code and returns JWT token to API Gateway
     */
    @PostMapping("/oauth/github")
    public ResponseEntity<OAuthResponse> oauthGithub(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // Process GitHub OAuth code and get/create user
            User user = githubOAuthService.processCode(code);

            // Generate JWT token
            String jwt = jwtService.generateJwtToken(
                    new JwtUserDto(user.getId(), user.getName(), user.getEmail())
            );

            // ═══ KAFKA: Publish user login event (GitHub) ═══
            kafkaEventPublisher.publishUserLoginEvent(
                    new UserLoginEvent(
                            user.getId(),
                            user.getEmail(),
                            "github",
                            System.currentTimeMillis()
                    )
            );
            // ════════════════════════════════════════════════

            // Return JWT to API Gateway
            return ResponseEntity.ok(new OAuthResponse(jwt));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * User signup endpoint
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {

        User user = userService.signUp(signupRequest);

        // Generate JWT token
        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail())
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);

        // ═══ KAFKA: Publish user signup event ═══
        kafkaEventPublisher.publishUserSignupEvent(
                new UserSignupEvent(
                        user.getId(),
                        user.getEmail(),
                        user.getName(),
                        System.currentTimeMillis()
                )
        );
        // ═════════════════════════════════════════

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(resp, "Signup successful"));
    }

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        User user = userService.login(loginRequest);

        // Generate JWT token
        String jwt = jwtService.generateJwtToken(
                new JwtUserDto(user.getId(), user.getName(), user.getEmail())
        );

        SuccessfulLoginResponse resp = userMapper.toResponse(user, jwt);

        // ═══ KAFKA: Publish user login event (Email/Password) ═══
        kafkaEventPublisher.publishUserLoginEvent(
                new UserLoginEvent(
                        user.getId(),
                        user.getEmail(),
                        "email",
                        System.currentTimeMillis()
                )
        );
        // ═══════════════════════════════════════════════════════

        return ResponseEntity
                .ok(new ApiResponse<>(resp, "Login successful"));
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> getCurrentUser(
            @AuthenticationPrincipal JwtUserDto user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User dbUser = userService.findById(user.id())
                .orElseThrow(() -> new NotFoundException("User not found"));

        SuccessfulLoginResponse resp = userMapper.toResponse(dbUser, null);
        return ResponseEntity.ok(new ApiResponse<>(resp, "User fetched successfully"));
    }
}