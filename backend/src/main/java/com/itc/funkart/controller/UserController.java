package com.itc.funkart.controller;

import com.itc.funkart.dto.user.LoginRequest;
import com.itc.funkart.dto.user.SignupRequest;
import com.itc.funkart.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.entity.User;
import com.itc.funkart.mapper.user.UserMapper;
import com.itc.funkart.response.ApiResponse;
import com.itc.funkart.security.CookieUtil;
import com.itc.funkart.service.JwtService;
import com.itc.funkart.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.version}/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;

    public UserController(UserService userService,
                          UserMapper userMapper,
                          JwtService jwtService,
                          CookieUtil cookieUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    // -----------------------------
    // SIGNUP
    // -----------------------------
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletResponse response) {

        User user = userService.signUp(signupRequest);

        // generate JWT immediately after signup
        String token = jwtService.generateJwtToken(user);

        // set cookie
        cookieUtil.addTokenCookie(response, token, 3600);

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
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        User user = userService.login(loginRequest);

        String token = jwtService.generateJwtToken(user);

        cookieUtil.addTokenCookie(response, token, 3600);

        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

        return ResponseEntity
                .ok(new ApiResponse<>(resp, "Login successful"));
    }

    // -----------------------------
    // CURRENT USER
    // -----------------------------
    @GetMapping("/me")
    public ResponseEntity<SuccessfulLoginResponse> getCurrentUser() {

        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);

        return ResponseEntity.ok(resp);
    }
}