package com.itc.funkart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubOAuthConfig {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GithubOAuthConfig(
            @Value("${github.oauth.client-id}") String clientId,
            @Value("${github.oauth.client-secret}") String clientSecret,
            @Value("${github.oauth.redirect-uri}") String redirectUri
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }
}