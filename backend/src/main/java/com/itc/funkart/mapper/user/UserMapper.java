package com.itc.funkart.mapper.user;

import com.itc.funkart.dto.user.SuccessfulLoginResponse;
import com.itc.funkart.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class to map user details to a dto upon successful signup/login
 * used to abstract away sensitive information e.g. passwords
 */
@Component
public class UserMapper {
    public SuccessfulLoginResponse toResponse(User user, String token) {
        return new SuccessfulLoginResponse(user.getId(), user.getEmail(), user.getName(), token);
    }
}
