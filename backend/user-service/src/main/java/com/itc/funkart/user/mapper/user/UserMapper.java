package com.itc.user.mapper.user;

import com.itc.user.dto.user.SuccessfulLoginResponse;
import com.itc.user.entity.User;
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
