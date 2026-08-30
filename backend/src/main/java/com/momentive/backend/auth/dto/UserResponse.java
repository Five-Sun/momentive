package com.momentive.backend.auth.dto;

import com.momentive.backend.auth.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
        @Schema(description = "사용자 ID") Long id,
        @Schema(description = "이메일") String email,
        @Schema(description = "닉네임") String nickname
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}
