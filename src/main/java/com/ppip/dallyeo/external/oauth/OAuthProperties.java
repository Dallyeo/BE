package com.ppip.dallyeo.external.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 설정 (NFR tech-stack). 시크릿성 값(apple.audience)은 env 주입.
 * 활성화는 {@code @EnableConfigurationProperties}(SecurityConfig)에서.
 */
@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(
        Kakao kakao,
        Apple apple
) {
    /** Kakao: access token으로 사용자 조회(별도 client secret 불필요). */
    public record Kakao(String userInfoUri) {
    }

    /** Apple: identity token(JWT)을 JWKS로 검증. audience = client_id(env). */
    public record Apple(String jwksUri, String issuer, String audience) {
    }
}
