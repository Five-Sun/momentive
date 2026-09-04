package com.momentive.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.momentive.backend.auth.domain.Role;
import com.momentive.backend.auth.security.AccessTokenPayload;
import com.momentive.backend.auth.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-secret-key-for-jwt-token-provider-unit-test";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(SECRET);

    @Test
    void access_token_carries_role_claim() {
        String token = jwtTokenProvider.createAccessToken(7L, Role.ADMIN);

        Optional<AccessTokenPayload> payload = jwtTokenProvider.parseAccessToken(token);

        assertThat(payload).contains(new AccessTokenPayload(7L, Role.ADMIN));
    }

    @Test
    void access_token_without_role_claim_falls_back_to_user() {
        String legacyToken = legacyAccessTokenWithoutRole(42L);

        Optional<AccessTokenPayload> payload = jwtTokenProvider.parseAccessToken(legacyToken);

        assertThat(payload).contains(new AccessTokenPayload(42L, Role.USER));
    }

    @Test
    void refresh_token_is_not_accepted_as_access_token() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);

        assertThat(jwtTokenProvider.parseAccessToken(refreshToken)).isEmpty();
        assertThat(jwtTokenProvider.parseRefreshTokenSubject(refreshToken)).contains(1L);
    }

    @Test
    void invalid_token_is_rejected() {
        assertThat(jwtTokenProvider.parseAccessToken("not-a-real-token")).isEmpty();
    }

    /**
     * role 클레임이 추가되기 전에 발급된 access token을 재현한다.
     */
    private String legacyAccessTokenWithoutRole(Long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(30, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }
}
