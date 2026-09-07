package com.momentive.backend.auth.security;

import com.momentive.backend.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;

    public JwtTokenProvider(@Value("${momentive.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createAccessToken(Long userId, Role role) {
        Instant now = Instant.now();
        return baseToken(userId, TYPE_ACCESS, ACCESS_TOKEN_TTL, now)
                .claim(CLAIM_ROLE, role.name())
                .compact();
    }

    /**
     * refresh token은 권한을 싣지 않는다 — 권한 판정은 access token으로만 한다.
     */
    public String createRefreshToken(Long userId) {
        return baseToken(userId, TYPE_REFRESH, REFRESH_TOKEN_TTL, Instant.now()).compact();
    }

    public Duration getAccessTokenTtl() {
        return ACCESS_TOKEN_TTL;
    }

    public Duration getRefreshTokenTtl() {
        return REFRESH_TOKEN_TTL;
    }

    private JwtBuilder baseToken(Long userId, String type, Duration ttl, Instant now) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key);
    }

    /**
     * 유효한 access token이면 사용자 id와 권한을, 아니면 empty를 반환한다.
     */
    public Optional<AccessTokenPayload> parseAccessToken(String token) {
        return parseClaimsIfType(token, TYPE_ACCESS)
                .flatMap(claims -> parseSubject(claims)
                        .map(userId -> new AccessTokenPayload(userId, resolveRole(claims))));
    }

    public Optional<Long> parseRefreshTokenSubject(String token) {
        return parseClaimsIfType(token, TYPE_REFRESH).flatMap(this::parseSubject);
    }

    private Optional<Claims> parseClaimsIfType(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<Long> parseSubject(Claims claims) {
        try {
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * role 클레임이 없는 구 토큰(이 변경 이전에 발급된 토큰)은 USER로 취급한다.
     * 관리자로 승격된 계정도 재로그인 전까지는 최대 30분(access token TTL) 동안 USER로 보인다.
     */
    private Role resolveRole(Claims claims) {
        try {
            String role = claims.get(CLAIM_ROLE, String.class);
            return role == null ? Role.USER : Role.valueOf(role);
        } catch (JwtException | IllegalArgumentException e) {
            return Role.USER;
        }
    }
}
