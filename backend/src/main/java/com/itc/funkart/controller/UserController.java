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

    public UserController(UserService userService, UserMapper userMapper, JwtService jwtService, CookieUtil cookieUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        // Call the service to signup
        User user = userService.signUp(signupRequest);
        // Map entity -> DTO to hide the password
        SuccessfulLoginResponse response = userMapper.toResponse(user, null); //No token yet
        // Wrap in ApiResponse and return it
        ApiResponse<SuccessfulLoginResponse> apiResponse =
                new ApiResponse<>(response, "User created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SuccessfulLoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                      HttpServletResponse response) {
        //Authenticate user
        User user = userService.login(loginRequest);
        //Generate JWT
        String token = jwtService.generateJwtToken(user);
        //Set JWT as HttpOnly cookie
        cookieUtil.addTokenCookie(response, token, 3600); // maxAge = 1 hour
        //Map entity -> DTO (without sending token in body)
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);
        //Wrap in ApiResponse
        ApiResponse<SuccessfulLoginResponse> apiResponse = new ApiResponse<>(resp, "Login successful");
        return ResponseEntity.ok(apiResponse);
    }
    @GetMapping("/me")
    public ResponseEntity<SuccessfulLoginResponse> getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SuccessfulLoginResponse resp = userMapper.toResponse(user, null);
        return ResponseEntity.ok(resp);
    }
}