package com.ppip.dallyeo.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 인증 사용자 ID(Long)를 주입 (nfr-design 2, AuthUserArgumentResolver).
 * 보호(🔒) 엔드포인트에서만 유효 — 미인증이면 UNAUTHORIZED.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
