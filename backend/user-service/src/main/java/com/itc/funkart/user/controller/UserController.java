package com.itc.funkart.user.controller;

import com.itc.funkart.common.dto.response.ApiResponse;
import com.itc.funkart.common.dto.security.UserPrincipalDto;
import com.itc.funkart.common.dto.user.UserProfileDto;
import com.itc.funkart.user.auth.AuthFacadeService;
import com.itc.funkart.user.dto.user.*;
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
 * <p>Thin API layer responsible for request routing and response wrapping.
 * Delegates all business logic to AuthFacadeService and UserService.</p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthFacadeService authFacadeService;

    /**
     * Entry point for GitHub OAuth authentication.
     * Called internally by the API Gateway after it receives the GitHub callback.
     * Gateway posts the code as JSON body: { "code": "..." }
     */
    @PostMapping("/oauth/github")
    public ResponseEntity<ApiResponse<OAuthResponse>> githubOAuth(
            @Valid @RequestBody OAuthRequest oAuthRequest
    ) {
        OAuthResponse data = authFacadeService.handleGithubLogin(oAuthRequest.code());
        return ResponseEntity.ok(ApiResponse.success(data, "GitHub login successful"));
    }

    /**
     * Standard Email/Password registration.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest
    ) {
        SuccessfulLoginResponse data = authFacadeService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Signup successful"));
    }

    /**
     * Standard Email/Password login.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        SuccessfulLoginResponse data = authFacadeService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    /**
     * Fetches the current authenticated user's profile.
     * Uses the principal injected by the JwtAuthWebFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipalDto principalUser
    ) {
        // Safe check for the JVM security context
        if (principalUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserProfileDto profile = userService.getUserProfile(principalUser.userId());
        return ResponseEntity.ok(ApiResponse.success(profile, "User profile fetched successfully"));
    }

    /**
     * Updates the authenticated user's display name.
     * Body: { "name": "New Name" }
     */
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipalDto principalUser,
            @RequestBody java.util.Map<String, String> body
    ) {
        if (principalUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserProfileDto updated = userService.updateProfile(principalUser.userId(), body.get("name"));
        return ResponseEntity.ok(ApiResponse.success(updated, "Profile updated"));
    }

    /**
     * Changes the authenticated user's password.
     * Body: { "currentPassword": "...", "newPassword": "..." }
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipalDto principalUser,
            @RequestBody java.util.Map<String, String> body
    ) {
        if (principalUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        userService.changePassword(principalUser.userId(), body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}