package com.ppip.dallyeo.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 설정 바인딩 (NFR tech-stack, BR-1). secret은 env 주입(코드/VCS 저장 금지).
 * 만료는 ms. Access 24h / Refresh 7d(US-AUTH-2).
 * 활성화는 {@code @EnableConfigurationProperties}(SecurityConfig)에서.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessExpiration,
        long refreshExpiration
) {
}
