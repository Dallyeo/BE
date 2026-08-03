package com.ppip.dallyeo.external.oauth;

import com.ppip.dallyeo.user.Provider;

/**
 * 소셜 검증 결과(비영속 값 객체). 이메일 등은 수집하지 않음(Q3=A 최소 수집).
 * nickname은 소셜 프로필 닉네임(없으면 null → 자동생성 대상).
 */
public record OAuthUser(
        Provider provider,
        String providerUserId,
        String nickname
) {
}
