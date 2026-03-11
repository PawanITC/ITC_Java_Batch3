package com.itc.funkart.controller;

import com.itc.funkart.config.GithubOAuthConfig;
import com.itc.funkart.entity.User;
import com.itc.funkart.exceptions.OAuthException;
import com.itc.funkart.security.CookieUtil;
import com.itc.funkart.service.GithubOAuthService;
import com.itc.funkart.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final GithubOAuthConfig config;
    private final GithubOAuthService githubOAuthService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    @Value("${frontend.url}")
    private String frontendUrl;

    public GithubOAuthController(GithubOAuthConfig config,
                                 GithubOAuthService githubOAuthService,
                                 JwtService jwtService,
                                 CookieUtil cookieUtil) {
        this.config = config;
        this.githubOAuthService = githubOAuthService;
        this.jwtService = jwtService;
        this.cookieUtil = cookieUtil;
    }

    /**
     * Redirects user to GitHub OAuth login page
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + config.getClientId()
                + "&scope=user:email"
                + "&redirect_uri=" + config.getRedirectUri();
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .header("Location", url)
                .build();
    }

    /**
     * Handles GitHub OAuth callback and sets JWT cookie
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, HttpServletResponse response) {
        try {
            User user = githubOAuthService.processCode(code);
            String jwt = jwtService.generateJwtToken(user);

            // Use default max age from CookieUtil
            cookieUtil.addTokenCookie(response, jwt, null);

            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                    .header("Location", frontendUrl)
                    .build();
        } catch (OAuthException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Logout by clearing JWT cookie
     */
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookieUtil.clearTokenCookie(response);
        return ResponseEntity.ok().build();
    }
}