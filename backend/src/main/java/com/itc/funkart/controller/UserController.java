package com.itc.funkart.controller;

import com.itc.funkart.dto.user.LoginRequest;
import com.itc.funkart.dto.user.SignupRequest;
import com.itc.funkart.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.entity.User;
import com.itc.funkart.mapper.user.UserMapper;
import com.itc.funkart.response.ApiResponse;
import com.itc.funkart.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.version}/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        // Call the service to signup
        User user = userService.signUp(signupRequest);
        // Map entity -> DTO to hide the password
        SuccessfulLoginResponse response = userMapper.toResponse(user);
        // Wrap in ApiResponse and return it
        ApiResponse<SuccessfulLoginResponse> apiResponse =
                new ApiResponse<>(response, "User created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // Call the service to login
        User user = userService.login(loginRequest);
        // Map entity -> DTO to hide the password
        SuccessfulLoginResponse response = userMapper.toResponse(user);
        // Wrap in ApiResponse and return it
        ApiResponse<SuccessfulLoginResponse> apiResponse =
                new ApiResponse<>(response, "Login successful");
        return ResponseEntity.ok(apiResponse);
    }
}
