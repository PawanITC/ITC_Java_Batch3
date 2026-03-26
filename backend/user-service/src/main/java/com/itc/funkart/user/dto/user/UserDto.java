package com.itc.funkart.user.dto.user;

import lombok.Getter;
import lombok.Setter;

/**
 * UserDto for internal service-to-service communication
 * Used by gateway to receive user data after OAuth processing
 */
@Setter
@Getter
public class UserDto {
    private Long id;
    private String name;
    private String email;

    public UserDto() {}

    public UserDto(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
}