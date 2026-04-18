package com.itc.funkart.user.controller;

import com.itc.funkart.user.auth.AuthFacadeService;
import com.itc.funkart.user.dto.security.UserPrincipalDto;
import com.itc.funkart.user.dto.user.*;
import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>User Controller</h2>
 *
 * <p>
 * Thin API layer responsible ONLY for request routing and response wrapping.
 * Delegates all business logic to services.
 * </p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthFacadeService authFacadeService;

    @PostMapping("/oauth/github")
    public ResponseEntity<ApiResponse<OAuthResponse>> oauthGithub(
            @RequestBody OAuthRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        authFacadeService.handleGithubLogin(request.code()),
                        "GitHub login successful"
                )
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest
    ) {
        SuccessfulLoginResponse response = authFacadeService.signup(signupRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(response, "Signup successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        SuccessfulLoginResponse response = authFacadeService.login(loginRequest);

        return ResponseEntity.ok(
                new ApiResponse<>(response, "Login successful")
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipalDto principalUser
    ) {
        if (principalUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserProfileDto profile = userService.getUserProfile(principalUser.userId());

        return ResponseEntity.ok(
                new ApiResponse<>(profile, "User profile fetched successfully")
        );
    }
}