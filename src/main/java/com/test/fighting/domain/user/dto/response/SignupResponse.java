package com.test.fighting.domain.user.dto.response;

import com.test.fighting.domain.user.entity.User;

public record SignupResponse(Long id, String username, String nickname) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
