package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.OAuthResponse;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.NotFoundException;
import com.itc.funkart.user.mapper.user.UserMapper;
import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.service.GithubOAuthService;
import com.itc.funkart.user.service.JwtService;
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

    public UserController(UserService userService,
                          GithubOAuthService githubOAuthService,
                          UserMapper userMapper,
                          JwtService jwtService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.githubOAuthService = githubOAuthService;
        this.jwtService = jwtService;
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
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

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
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

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