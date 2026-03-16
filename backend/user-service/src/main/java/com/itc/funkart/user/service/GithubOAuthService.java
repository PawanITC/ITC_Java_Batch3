package com.itc.funkart.user.service;

import com.itc.funkart.user.config.GithubOAuthConfig;
import com.itc.funkart.user.dto.github.AccessTokenResponse;
import com.itc.funkart.user.dto.github.GithubUser;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.OAuthException;  // ← ADD THIS
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
@Transactional
public class GithubOAuthService {

    private final GithubOAuthConfig config;
    private final WebClient webClient;
    private final OAuthAccountService oauthAccountService;
    private final UserService userService;

    public GithubOAuthService(
            GithubOAuthConfig config,
            OAuthAccountService oauthAccountService,
            UserService userService,
            WebClient webClient
    ) {
        this.config = config;
        this.oauthAccountService = oauthAccountService;
        this.userService = userService;
        this.webClient = webClient;
    }

    public User processCode(String code) {

        String accessToken = getAccessToken(code);
        GithubUser githubUser = fetchGithubUser(accessToken);

        // GitHub email may be null
        String email = githubUser.email();
        if (email == null || email.isBlank()) {
            email = githubUser.login() + "@github.oauth";
        }

        String name = (githubUser.name() != null && !githubUser.name().isBlank())
                ? githubUser.name()
                : githubUser.login();

        // Find or create user
        String finalEmail = email;
        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.createUser(
                        finalEmail,
                        null, // OAuth users have no password
                        name
                ));

        // Link OAuth account
        oauthAccountService.findOrCreate(
                user.getId(),
                "github",
                githubUser.id().toString()
        );

        return user;
    }

    private String getAccessToken(String code) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", config.getRedirectUri());

        AccessTokenResponse response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(AccessTokenResponse.class)
                .block();

        if (response == null || response.access_token() == null) {
            throw new OAuthException("Failed to retrieve GitHub access token");  // ← CHANGE THIS
        }

        return response.access_token();
    }

    private GithubUser fetchGithubUser(String accessToken) {

        GithubUser user = webClient.get()
                .uri("https://api.github.com/user")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubUser.class)
                .block();

        if (user == null) {
            throw new OAuthException("Failed to fetch GitHub user info");  // ← CHANGE THIS
        }

        return user;
    }
}