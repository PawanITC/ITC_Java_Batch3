package com.itc.funkart.service;

import com.itc.funkart.config.GithubOAuthConfig;
import com.itc.funkart.dto.github.AccessTokenRequest;
import com.itc.funkart.dto.github.AccessTokenResponse;
import com.itc.funkart.dto.github.GithubUser;
import com.itc.funkart.entity.OAuthAccount;
import com.itc.funkart.entity.User;
import com.itc.funkart.repository.OAuthAccountRepository;
import com.itc.funkart.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GithubOAuthService {

    private final GithubOAuthConfig config;
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final JwtService jwtService;

    public GithubOAuthService(GithubOAuthConfig config,
                              UserRepository userRepository,
                              OAuthAccountRepository oauthAccountRepository,
                              JwtService jwtService,
                              WebClient webClient) {
        this.config = config;
        this.webClient = webClient;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.jwtService = jwtService;
    }

    public String processGithubLogin(String code) {
        // 1. Exchange code for access token
        String accessToken = getAccessToken(code);

        // 2. Fetch GitHub user info
        GithubUser githubUser = fetchGithubUser(accessToken);

        // 3. Find or create User & OAuthAccount
        User user = findOrCreateUser(githubUser);

        // 4. Generate JWT
        return jwtService.generateToken(user);
    }

    private String getAccessToken(String code) {
        AccessTokenResponse response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON) // request JSON response
                .bodyValue(new AccessTokenRequest(
                        config.getClientId(),
                        config.getClientSecret(),
                        code,
                        config.getRedirectUri()
                ))
                .retrieve()
                .bodyToMono(AccessTokenResponse.class)
                .block(); // block is fine here because it's a backend exchange

        if (response == null || response.access_token() == null) {
            throw new RuntimeException("Failed to retrieve GitHub access token");
        }

        return response.access_token();
    }

    private GithubUser fetchGithubUser(String accessToken) {
        return webClient.get()
                .uri("https://api.github.com/user")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GithubUser.class)
                .block();
    }

    private User findOrCreateUser(GithubUser githubUser) {
        OAuthAccount account = oauthAccountRepository.findByProviderAndProviderId("github", githubUser.id())
                .orElse(null);

        if (account != null) {
            return account.getUser();
        }

        // Create new user
        User user = new User();
        user.setEmail(githubUser.email() != null ? githubUser.email() : githubUser.login());
        user.setName(githubUser.name() != null ? githubUser.name() : githubUser.login());
        user.setPassword(""); // no password since GitHub OAuth

        user = userRepository.save(user);

        // Create OAuthAccount
        OAuthAccount newAccount = new OAuthAccount();
        newAccount.setProvider("github");
        newAccount.setProviderId(githubUser.id());
        newAccount.setUser(user);

        oauthAccountRepository.save(newAccount);

        return user;
    }
}