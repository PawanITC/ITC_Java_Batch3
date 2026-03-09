package com.itc.funkart.controller;

import com.itc.funkart.config.GithubOAuthConfig;
import com.itc.funkart.service.GithubOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth/github")
public class GithubOAuthController {

    private final GithubOAuthConfig config;
    private final GithubOAuthService  githubOAuthService;
    public GithubOAuthController(GithubOAuthConfig config, GithubOAuthService  githubOAuthService) {
        this.config = config;
        this.githubOAuthService= githubOAuthService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {

        String url = "https://github.com/login/oauth/authorize"
                + "?client_id=" + config.getClientId()
                + "&scope=user:email"
                + "&redirect_uri=" + config.getRedirectUri();

        return ResponseEntity
                .status(302)
                .header("Location", url)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        String jwt = githubOAuthService.processGithubLogin(code);
        return ResponseEntity.ok(jwt); // frontend stores JWT
    }
}
