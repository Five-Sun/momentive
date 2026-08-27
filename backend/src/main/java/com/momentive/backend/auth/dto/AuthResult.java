package com.momentive.backend.auth.dto;

/**
 * 회원가입/로그인/refresh 성공 시 Service가 Controller에 돌려주는 결과.
 * 사용자 정보(응답 바디용)와 토큰 원문(쿠키 세팅용)을 함께 담는다.
 */
public record AuthResult(UserResponse user, TokenPair tokens) {
}
