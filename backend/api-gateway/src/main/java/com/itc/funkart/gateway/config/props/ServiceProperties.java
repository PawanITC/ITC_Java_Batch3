package com.itc.funkart.gateway.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "services")
public record ServiceProperties(
        Map<String, String> urls
) {}