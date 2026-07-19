package com.ppip.dallyeo.external.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * TourAPI 설정 바인딩 (domain-entities §5, N3).
 * serviceKey는 환경변수/시크릿으로 주입되며 코드·문서·VCS에 값 노출 금지.
 * 활성화는 {@code @EnableConfigurationProperties}(RestClientConfig)에서.
 */
@ConfigurationProperties(prefix = "tourapi")
public record TourApiProperties(
        String baseUrl,
        String serviceKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl
) {
}
