package com.momentive.backend.auth.security;

import com.momentive.backend.auth.domain.Role;

/**
 * access token 파싱 결과. 사용자 id와 토큰에 실린 권한을 함께 담는다.
 */
public record AccessTokenPayload(Long userId, Role role) {
}
