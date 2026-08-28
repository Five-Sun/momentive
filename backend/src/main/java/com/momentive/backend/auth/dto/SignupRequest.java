package com.momentive.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
                message = "비밀번호는 최소 8자, 영문/숫자 조합이어야 합니다."
        ) String password,
        @NotBlank String nickname
) {
}
