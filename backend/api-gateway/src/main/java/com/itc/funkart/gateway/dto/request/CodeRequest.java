package com.itc.funkart.gateway.dto.request;


/**
 * Request body sent to User-Service for OAuth processing.
 * Contains the authorization code received from GitHub.
 */
public record CodeRequest(String code) {}