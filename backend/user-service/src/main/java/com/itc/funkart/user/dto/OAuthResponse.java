package com.itc.funkart.user.dto;

/**
 * Response sent back to API Gateway after OAuth processing.
 * Contains the JWT token that the gateway will set as a cookie.
 */
public record OAuthResponse(String token) {}