package com.ppip.dallyeo.auth.dev;

/**
 * dev 로그인 요청. providerUserId 미지정 시 기본 테스트 사용자("tester-1") 사용.
 */
public record DevLoginRequest(String providerUserId) {
}
