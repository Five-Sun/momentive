package com.momentive.backend.auth.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 로그인 사용자의 id(Long)를 주입하되,
 * 비로그인 상태여도 예외를 던지지 않고 null을 주입한다.
 * 비로그인도 접근 가능해야 하지만 로그인 시 본인 여부(isMine 등) 판단이 필요한 엔드포인트에서 사용한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalCurrentUser {
}
