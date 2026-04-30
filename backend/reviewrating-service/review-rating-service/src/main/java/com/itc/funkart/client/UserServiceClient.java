package com.itc.funkart.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackUser")
    public String getUser(String userId) {
        return restTemplate.getForObject(
                "http://user-service/users/" + userId,
                String.class
        );
    }

    public String fallbackUser(String userId, Throwable ex) {
        return "DEFAULT_USER";
    }
}
