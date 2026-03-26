package com.itc.funkart.user.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginEvent {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("login_method")
    private String loginMethod; // "email" or "github"

    @JsonProperty("timestamp")
    private Long timestamp;
}
