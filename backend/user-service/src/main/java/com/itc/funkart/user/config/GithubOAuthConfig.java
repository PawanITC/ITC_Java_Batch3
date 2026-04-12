package com.itc.funkart.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for GitHub OAuth2 integration.
 * Values are mapped from the {@code github.oauth} prefix in {@code application.yml}.
 * * @param clientId     The GitHub application client ID.
 * @param clientSecret The GitHub application secret key.
 * @param redirectUri  The authorized callback URL.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "github.oauth")
public class GithubOAuthConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
}