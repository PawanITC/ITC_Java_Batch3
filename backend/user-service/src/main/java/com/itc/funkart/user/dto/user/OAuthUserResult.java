package com.itc.funkart.user.dto.user;

import com.itc.funkart.user.entity.User;

public record OAuthUserResult(User user, boolean isNew) {}
