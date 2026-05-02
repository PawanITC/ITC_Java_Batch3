package com.itc.funkart.common.dto.auth.response;

import com.itc.funkart.common.dto.user.UserDto;
import lombok.Builder;

/**
 * Response wrapper for successful authentication.
 * Maps the nested UserDto and the accompanying JWT token.
 */
@Builder
public record SuccessfulLoginResponse(
        UserDto user,
        String token
) {
}
