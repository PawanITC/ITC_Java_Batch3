package com.itc.funkart.controller;

import com.itc.funkart.config.GithubOAuthConfig;
import com.itc.funkart.security.CookieUtil;
import com.itc.funkart.service.GithubOAuthService;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CookieUtil cookieUtil;

    public GithubOAuthController(GithubOAuthConfig config,
                                 GithubOAuthService githubOAuthService,
                                 CookieUtil cookieUtil) {
        this.config = config;
        this.githubOAuthService = githubOAuthService;
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
    public void githubCallback(@RequestParam String code, HttpServletResponse response) {
        String jwt = githubOAuthService.processGithubLogin(code);

        // Set cookie for 1 hour (3600s)
        cookieUtil.addTokenCookie(response, jwt, 3600);

        // Redirect to frontend
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", "http://localhost:5173/");
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