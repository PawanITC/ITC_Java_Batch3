package com.itc.funkart.user.controller;

import com.itc.funkart.user.dto.user.LoginRequest;
import com.itc.funkart.user.dto.user.SignupRequest;
import com.itc.funkart.user.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.NotFoundException;
import com.itc.funkart.user.mapper.user.UserMapper;
import com.itc.funkart.user.response.ApiResponse;
import com.itc.funkart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version}/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService,
                          UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // -----------------------------
    // SIGNUP
    // -----------------------------
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {

        User user = userService.signUp(signupRequest);
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(resp, "Signup successful"));
    }

    // -----------------------------
    // LOGIN
    // -----------------------------
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        User user = userService.login(loginRequest);
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

        return ResponseEntity
                .ok(new ApiResponse<>(resp, "Login successful"));
    }

    // -----------------------------
    // CURRENT USER (optional)
    // -----------------------------
    @GetMapping("/me")
    public ResponseEntity<SuccessfulLoginResponse> getCurrentUser(@RequestParam Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);
        return ResponseEntity.ok(resp);
    }
}