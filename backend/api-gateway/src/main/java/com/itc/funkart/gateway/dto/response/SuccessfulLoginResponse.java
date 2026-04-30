package com.itc.funkart.gateway.dto.response;

import com.itc.funkart.gateway.dto.UserDto;
import lombok.Builder;

/**
 * Response wrapper for successful authentication.
 * Maps the nested UserDto and the accompanying JWT token.
 */
@Builder
public record SuccessfulLoginResponse(
        UserDto user,
        String token
) {}