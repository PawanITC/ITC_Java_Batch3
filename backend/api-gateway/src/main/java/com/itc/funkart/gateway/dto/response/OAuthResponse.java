package com.itc.funkart.gateway.dto.response;


/**
 * Response received from User-Service after OAuth processing.
 * Contains the JWT token to be set as a cookie and sent to frontend.
 */
public record OAuthResponse(String token) {}
