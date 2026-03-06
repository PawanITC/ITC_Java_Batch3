package com.itc.funkart.controller;

import com.itc.funkart.dto.user.LoginRequest;
import com.itc.funkart.dto.user.SignupRequest;
import com.itc.funkart.entity.User;
import com.itc.funkart.response.ApiResponse;
import com.itc.funkart.service.UserService;
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

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<User>> signup(@RequestBody SignupRequest signupRequest) {
        //attempt to signup user (business logic handles errors)
        User user = userService.signUp(signupRequest);
        //set up api response object that will be passed in response entity
        ApiResponse<User> apiResponse = new ApiResponse<>(true, "User created successfully", user);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<User>> login(@RequestBody LoginRequest loginRequest) {
        //attempt to signup user (business logic handles errors)
        User user = userService.login(loginRequest);
        ApiResponse<User> apiResponse = new ApiResponse<>(true, "Login successful", user);
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }
}
