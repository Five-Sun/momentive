package com.momentive.backend.auth.controller;

import com.momentive.backend.auth.dto.AuthResult;
import com.momentive.backend.auth.dto.LoginRequest;
import com.momentive.backend.auth.dto.SignupRequest;
import com.momentive.backend.auth.dto.UserResponse;
import com.momentive.backend.auth.security.AuthCookieProvider;
import com.momentive.backend.auth.security.CurrentUser;
import com.momentive.backend.auth.security.JwtTokenProvider;
import com.momentive.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProvider authCookieProvider;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        AuthResult result = authService.signup(request);
        setAuthCookies(response, result);
        return result.user();
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        setAuthCookies(response, result);
        return result.user();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieProvider.getRefreshToken(request).ifPresent(authService::logout);
        authCookieProvider.expireAuthCookies(response);
    }

    @PostMapping("/refresh")
    public UserResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieProvider.getRefreshToken(request).orElse(null);
        AuthResult result = authService.refresh(refreshToken);
        setAuthCookies(response, result);
        return result.user();
    }

    @GetMapping("/me")
    public UserResponse me(@CurrentUser Long userId) {
        return authService.me(userId);
    }

    private void setAuthCookies(HttpServletResponse response, AuthResult result) {
        authCookieProvider.setAccessTokenCookie(response, result.tokens().accessToken(),
                jwtTokenProvider.getAccessTokenTtl().toSeconds());
        authCookieProvider.setRefreshTokenCookie(response, result.tokens().refreshToken(),
                jwtTokenProvider.getRefreshTokenTtl().toSeconds());
    }
}
