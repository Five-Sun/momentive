package com.momentive.backend.auth.dto;

/**
 * 발급된 Access/Refresh Token 원문. Service 내부에서 쿠키 세팅을 위해서만 사용하고
 * Controller 응답 바디로는 노출하지 않는다.
 */
public record TokenPair(String accessToken, String refreshToken) {
}
