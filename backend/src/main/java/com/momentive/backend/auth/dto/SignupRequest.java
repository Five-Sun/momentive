package com.momentive.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @Schema(description = "이메일") @NotBlank @Email String email,
        @Schema(description = "비밀번호 (최소 8자, 영문/숫자 조합)") @NotBlank @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
                message = "비밀번호는 최소 8자, 영문/숫자 조합이어야 합니다."
        ) String password,
        @Schema(description = "닉네임") @NotBlank String nickname
) {
}
