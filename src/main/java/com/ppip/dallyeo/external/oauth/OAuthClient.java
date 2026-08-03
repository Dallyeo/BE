package com.ppip.dallyeo.external.oauth;

import com.ppip.dallyeo.user.Provider;

/**
 * 소셜 검증 전략 인터페이스 (nfr-design 1.3). 제공자별 구현(Kakao/Apple).
 *
 * <p>현재는 FD Q2=A(프론트가 access token/identity token 전달 → 검증) 방식.
 * 훗날 authorization code 교환 방식으로 전환 시 <b>구현체 내부만 교체</b>하며 이 계약은 유지된다.
 */
public interface OAuthClient {

    /** 이 구현체가 담당하는 제공자. */
    Provider provider();

    /**
     * 소셜 credential(access token 또는 identity token)을 검증하고 사용자 정보를 반환.
     * 검증 실패/무효 → {@code BusinessException(UNAUTHORIZED)}.
     */
    OAuthUser verify(String credential);
}
