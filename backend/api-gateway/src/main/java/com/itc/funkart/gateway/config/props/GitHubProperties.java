package com.itc.funkart.gateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {}
