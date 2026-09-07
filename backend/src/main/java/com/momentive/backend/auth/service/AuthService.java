package com.momentive.backend.auth.service;

import com.momentive.backend.auth.domain.RefreshToken;
import com.momentive.backend.auth.domain.User;
import com.momentive.backend.auth.dto.AuthResult;
import com.momentive.backend.auth.dto.LoginRequest;
import com.momentive.backend.auth.dto.SignupRequest;
import com.momentive.backend.auth.dto.TokenPair;
import com.momentive.backend.auth.dto.UserResponse;
import com.momentive.backend.auth.repository.RefreshTokenRepository;
import com.momentive.backend.auth.repository.UserRepository;
import com.momentive.backend.auth.security.JwtTokenProvider;
import com.momentive.backend.common.exception.CustomException;
import com.momentive.backend.common.exception.ErrorCode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResult signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()));
        }
        User user = User.createUser(request.email(), passwordEncoder.encode(request.password()), request.nickname());
        userRepository.save(user);
        return issueAuthResult(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueAuthResult(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        jwtTokenProvider.parseRefreshTokenSubject(refreshToken);
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId = jwtTokenProvider.parseRefreshTokenSubject(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.isValid(LocalDateTime.now()) || !stored.getUser().getId().equals(userId)) {
            // 재사용 탐지: 이미 revoke되었거나 만료된 토큰이 다시 들어온 경우
            stored.revoke();
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        stored.revoke();
        User user = stored.getUser();
        return issueAuthResult(user);
    }

    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHENTICATED));
        return UserResponse.from(user);
    }

    private AuthResult issueAuthResult(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        RefreshToken entity = RefreshToken.issue(
                user,
                hash(refreshToken),
                LocalDateTime.now().plus(jwtTokenProvider.getRefreshTokenTtl())
        );
        refreshTokenRepository.save(entity);

        return new AuthResult(UserResponse.from(user), new TokenPair(accessToken, refreshToken));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
