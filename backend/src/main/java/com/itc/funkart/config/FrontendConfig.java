package com.itc.funkart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FrontendConfig {

    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    public String getFrontendUrl() {return frontendUrl;
    }
}