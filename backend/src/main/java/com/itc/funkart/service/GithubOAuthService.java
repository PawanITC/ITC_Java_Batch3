package com.itc.funkart.service;

import com.itc.funkart.config.GithubOAuthConfig;
import com.itc.funkart.dto.github.AccessTokenResponse;
import com.itc.funkart.dto.github.GithubUser;
import com.itc.funkart.entity.OAuthAccount;
import com.itc.funkart.entity.User;
import com.itc.funkart.exceptions.OAuthException;
import com.itc.funkart.repository.OAuthAccountRepository;
import com.itc.funkart.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Transactional
public class GithubOAuthService {

    private final GithubOAuthConfig config;
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;

    public GithubOAuthService(GithubOAuthConfig config,
                              UserRepository userRepository,
                              OAuthAccountRepository oauthAccountRepository,
                              WebClient webClient) {
        this.config = config;
        this.webClient = webClient;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
    }

    /**
     * Single atomic method: exchange code, fetch user, find/create user.
     */
    public User processCode(String code) {
        String accessToken = getAccessToken(code);
        GithubUser githubUser = fetchGithubUser(accessToken);
        return findOrCreateUser(githubUser);
    }

    private String getAccessToken(String code) {
        try {
            AccessTokenResponse response = webClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue("client_id=" + config.getClientId()
                            + "&client_secret=" + config.getClientSecret()
                            + "&code=" + code
                            + "&redirect_uri=" + config.getRedirectUri()
                    )
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                throw new OAuthException("Failed to retrieve GitHub access token");
            }

            return response.access_token();
        } catch (Exception ex) {
            throw new OAuthException("GitHub token exchange failed", ex);
        }
    }

    private GithubUser fetchGithubUser(String accessToken) {
        try {
            GithubUser user = webClient.get()
                    .uri("https://api.github.com/user")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GithubUser.class)
                    .block();

            if (user == null) {
                throw new OAuthException("Failed to fetch GitHub user info");
            }

            return user;
        } catch (Exception ex) {
            throw new OAuthException("GitHub API request failed", ex);
        }
    }

    private User findOrCreateUser(GithubUser githubUser) {
        return oauthAccountRepository.findByProviderAndProviderId("github", githubUser.id())
                .map(OAuthAccount::getUser)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(githubUser.email() != null ? githubUser.email() : githubUser.login());
                    user.setName(githubUser.name() != null ? githubUser.name() : githubUser.login());
                    user.setPassword(""); // OAuth login
                    user = userRepository.save(user);

                    OAuthAccount account = new OAuthAccount();
                    account.setProvider("github");
                    account.setProviderId(githubUser.id());
                    account.setUser(user);
                    oauthAccountRepository.save(account);

                    return user;
                });
    }
}