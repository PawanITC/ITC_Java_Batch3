package com.itc.funkart.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for GitHub OAuth2 integration.
 * Values are mapped from the {@code github.oauth} prefix in {@code application.yml}.
 * <p>
 * {@code  clientId}     The GitHub application client ID.
 * <p>
 * {@code clientSecret} The GitHub application secret key.
 * <p>
 * {@code redirectUri}  The authorized callback URL.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "github.oauth")
public class GithubOAuthConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUrl = "https://github.com/login/oauth/access_token";
    private String userApiUrl = "https://api.github.com/user";
}