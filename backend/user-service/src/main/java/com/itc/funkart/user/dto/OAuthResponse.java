package com.itc.funkart.user.dto;

import lombok.Builder;

/**
 * Specialized response containing a JWT token generated after successful OAuth processing.
 * * @param token The bearer token to be returned to the client/gateway.
 */
@Builder
public record OAuthResponse(String token) {}