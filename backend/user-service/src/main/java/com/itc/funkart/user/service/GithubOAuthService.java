package com.itc.funkart.user.service;

import com.itc.funkart.user.config.GithubOAuthConfig;
import com.itc.funkart.user.dto.github.AccessTokenResponse;
import com.itc.funkart.user.dto.github.GithubUser;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.OAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Service orchestrating the OAuth2 flow with GitHub.
 * Handles token exchange, user profile retrieval, and local account synchronization.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GithubOAuthService {

    private final GithubOAuthConfig config;
    private final WebClient webClient;
    private final OAuthAccountService oauthAccountService;
    private final UserService userService;

    /**
     * Executes the full GitHub OAuth handshake.
     * * @param code The temporary authorization code received from the GitHub callback.
     *
     * @return The local {@link User} entity associated with the GitHub profile.
     * @throws OAuthException if the token exchange or profile fetch fails.
     */
    public User processCode(String code) {
        String accessToken = getAccessToken(code);
        GithubUser githubUser = fetchGithubUser(accessToken);

        // Standardize email: Use GitHub email or fallback to a synthetic one
        String email = (githubUser.email() != null && !githubUser.email().isBlank())
                ? githubUser.email()
                : githubUser.login() + "@github.oauth";

        // Standardize name: Use display name or fallback to login handle
        String name = (githubUser.name() != null && !githubUser.name().isBlank())
                ? githubUser.name()
                : githubUser.login();

        // Atomic Find or Create local User
        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.createUser(email, null, name));

        // Link the external GitHub ID to the local user ID
        oauthAccountService.findOrCreate(
                user.getId(),
                "github",
                githubUser.id().toString()
        );

        return user;
    }

    /**
     * Exchanges the authorization code for a Bearer token.
     * Uses a try-catch to translate WebClient exceptions into business-level OAuthExceptions.
     */
    private String getAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", config.getRedirectUri());

        try {
            AccessTokenResponse response = webClient.post()
                    .uri("/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                throw new OAuthException("Failed to retrieve GitHub access token");
            }

            return response.access_token();
        } catch (Exception ex) {
            // This catch block hits the 'Actual: Unauthorized' scenario in your tests
            throw new OAuthException("Failed to retrieve GitHub access token: " + ex.getMessage());
        }
    }

    /**
     * Fetches the user's public profile from the GitHub API.
     */
    private GithubUser fetchGithubUser(String accessToken) {
        try {
            GithubUser user = webClient.get()
                    .uri("/user")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GithubUser.class)
                    .block();

            if (user == null) {
                throw new OAuthException("Failed to fetch GitHub user info");
            }

            return user;
        } catch (Exception ex) {
            throw new OAuthException("Failed to fetch GitHub user info: " + ex.getMessage());
        }
    }
}