package com.itc.funkart.user.auth;

import com.itc.funkart.user.auth.oauth.OAuthProvider;
import com.itc.funkart.user.config.GithubOAuthConfig;
import com.itc.funkart.user.dto.github.AccessTokenResponse;
import com.itc.funkart.user.dto.github.GithubUser;
import com.itc.funkart.user.dto.user.OAuthUserResult;
import com.itc.funkart.user.entity.User;
import com.itc.funkart.user.exceptions.OAuthException;
import com.itc.funkart.user.service.KafkaEventPublisher;
import com.itc.funkart.user.service.OAuthAccountService;
import com.itc.funkart.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * <h2>GitHub OAuth Orchestrator</h2>
 *
 * <p>
 * Responsible for executing the full GitHub OAuth login flow:
 * authorization code exchange, GitHub profile retrieval,
 * local user provisioning, and JWT issuance.
 * </p>
 *
 * <p>
 * This service acts as a high-level orchestrator and delegates
 * domain persistence and event publishing to dedicated services.
 * </p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GithubOAuthService {

    private final GithubOAuthConfig config;
    private final WebClient webClient;
    private final OAuthAccountService oauthAccountService;
    private final UserService userService;
    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Executes GitHub OAuth flow and returns a domain User.
     *
     * <p>
     * Responsibilities:
     * <ul>
     *   <li>Exchange authorization code for access token</li>
     *   <li>Fetch GitHub user profile</li>
     *   <li>Resolve or create local user</li>
     *   <li>Link OAuth account</li>
     * </ul>
     *
     * <p>
     * This service does NOT generate JWTs or return API DTOs.
     * </p>
     */
    public User processCode(String code) {

        String accessToken = getAccessToken(code);
        GithubUser githubUser = fetchGithubUser(accessToken);
        String githubProvider = OAuthProvider.GITHUB.value();

        String providerId = githubUser.id().toString();
        String email = normalizeEmail(githubUser);
        String username = githubUser.login();

        // 1. Resolve or create local user FIRST (single source of truth)
        OAuthUserResult result = userService.getOrCreateOAuthUser(email, username);

        // 2. Check if User already exists
        User user = result.user();
        boolean isNewUser = result.isNew();

        // ensure OAuth account exists
        oauthAccountService.findOrCreate(user, githubProvider, providerId);

        // 3. Only treat as "signup event" if user was newly created (you must derive this explicitly)
        //  Fire signup event ONLY if truly new user
        if (isNewUser) {
            kafkaEventPublisher.publishSignup(user);
        }

        return user;
    }

    //Helper methods


    private String getAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("redirect_uri", config.getRedirectUri());

        try {
            AccessTokenResponse response = webClient.post()
                    .uri(config.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();

            if (response == null || response.accessToken() == null) {
                throw new OAuthException("Failed to retrieve GitHub access token");
            }

            return response.accessToken();

        } catch (Exception ex) {
            throw new OAuthException("Failed to retrieve GitHub access token: " + ex.getMessage());
        }
    }

    private GithubUser fetchGithubUser(String accessToken) {
        try {
            GithubUser user = webClient.get()
                    .uri(config.getUserApiUrl())
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

    private String normalizeEmail(GithubUser githubUser) {
        return (githubUser.email() != null && !githubUser.email().isBlank())
                ? githubUser.email()
                : githubUser.login() + "@github.oauth";
    }

    private String normalizeName(GithubUser githubUser) {
        return (githubUser.name() != null && !githubUser.name().isBlank())
                ? githubUser.name()
                : githubUser.login();
    }
}