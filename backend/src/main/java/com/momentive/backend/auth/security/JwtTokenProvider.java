package com.momentive.backend.auth.security;

import io.jsonwebtoken.Claims;
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
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;

    public JwtTokenProvider(@Value("${momentive.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, TYPE_ACCESS, ACCESS_TOKEN_TTL);
    }

    public String createRefreshToken(Long userId) {
        return createToken(userId, TYPE_REFRESH, REFRESH_TOKEN_TTL);
    }

    public Duration getAccessTokenTtl() {
        return ACCESS_TOKEN_TTL;
    }

    public Duration getRefreshTokenTtl() {
        return REFRESH_TOKEN_TTL;
    }

    private String createToken(Long userId, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /**
     * 유효한 access token이면 사용자 id를, 아니면 empty를 반환한다.
     */
    public Optional<Long> parseAccessTokenSubject(String token) {
        return parseSubjectIfType(token, TYPE_ACCESS);
    }

    public Optional<Long> parseRefreshTokenSubject(String token) {
        return parseSubjectIfType(token, TYPE_REFRESH);
    }

    private Optional<Long> parseSubjectIfType(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
