package com.ppip.dallyeo.external.tourapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * TourAPI 설정 바인딩 (domain-entities §5, N3).
 * serviceKey는 환경변수/시크릿으로 주입되며 코드·문서·VCS에 값 노출 금지.
 * 활성화는 {@code @EnableConfigurationProperties}(RestClientConfig)에서.
 *
 * @param introCacheTtl    detailIntro2(영업시간) 전용 캐시 TTL. 목록 채우기가 항목당 1회 호출이라
 *                         공공데이터포털 <b>일일 한도</b>가 실질 제약이 된다. 영업시간은 하루 단위로
 *                         바뀌지 않으므로 기본 캐시(30분)보다 길게 잡아 호출량을 줄인다.
 * @param hoursRefreshAfter 보관 중인 영업시간을 다시 받아올 주기. 영업시간은 자주 바뀌지 않으므로
 *                          길게 잡아 일일 호출 한도를 아낀다.
 * @param introConcurrency 목록 영업시간 채우기용 detailIntro2 동시 호출 수(항목당 1회 호출이라 상한이 필요).
 * @param introBudget      목록 한 건이 영업시간 채우기에 쓸 수 있는 총 시간. 초과분은 businessHours=null로 응답.
 */
@ConfigurationProperties(prefix = "tourapi")
public record TourApiProperties(
        String baseUrl,
        String serviceKey,
        Duration connectTimeout,
        Duration readTimeout,
        Duration cacheTtl,
        Duration introCacheTtl,
        Duration hoursRefreshAfter,
        Integer introConcurrency,
        Duration introBudget
) {

    public TourApiProperties {
        introCacheTtl = introCacheTtl == null ? Duration.ofHours(24) : introCacheTtl;
        hoursRefreshAfter = hoursRefreshAfter == null ? Duration.ofDays(30) : hoursRefreshAfter;
        introConcurrency = (introConcurrency == null || introConcurrency < 1) ? 16 : introConcurrency;
        introBudget = introBudget == null ? Duration.ofSeconds(4) : introBudget;
    }
}
