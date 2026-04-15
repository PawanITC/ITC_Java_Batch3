package com.itc.funkart.gateway.dto.response;

import com.itc.funkart.gateway.dto.UserDto;
import lombok.Builder;

/**
 * Payload returned after a successful GitHub OAuth handshake.
 * Contains the mapped user profile and the system-generated JWT.
 */
@Builder
public record OAuthResponse(
        UserDto user,
        String token
) {}