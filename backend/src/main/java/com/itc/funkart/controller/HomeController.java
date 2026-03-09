package com.itc.funkart.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.itc.funkart.security.JwtUtil;
import com.itc.funkart.user.UserService;
import com.itc.funkart.user.User;
import com.itc.funkart.user.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;


@RestController
@RequestMapping("/")
public class HomeController {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;


    public HomeController(JwtUtil jwtUtil, UserService userService, PasswordEncoder passwordEncoder){
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping()
    public String HelloWorld(){
        return "Welcome this is a JWT login app please hit endpoint /login ";
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password,
                                   HttpServletResponse response)  {

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT using the "username" Spring Security knows (email in this case)
        String token = jwtUtil.generateToken(user);

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600);

        response.addCookie(cookie);

        return ResponseEntity.ok("Login successful");
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userService.save(user);

        return "User registered";
    }



}
