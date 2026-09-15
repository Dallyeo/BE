package com.ppip.dallyeo.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 응답에 싣는 이미지 경로를 <b>전체 URL</b>로 바꾼다.
 *
 * <p>우리가 서빙하는 이미지(코스·업적·러닝 기록)는 {@code /images/...}, {@code /uploads/...} 같은
 * 상대 경로인데, 장소 이미지는 TourAPI가 준 전체 URL이다. 섞여 나가면 클라이언트가 필드마다
 * "도메인을 붙여야 하나?"를 판단해야 하고, 이미 전체 URL인 값에 도메인을 또 붙이면 깨진다.
 * 그래서 서버가 전부 전체 URL로 통일해 내보낸다.
 *
 * <p>이미 {@code http://}/{@code https://} 로 시작하는 값은 그대로 둔다 — 그래서 장소 이미지에도
 * 안전하게 적용할 수 있다.
 *
 * <p>기준 도메인은 {@code app.public-base-url} 설정값이다. 프록시(nginx) 뒤에 있어
 * 요청 헤더에서 추정하면 {@code http}/{@code https} 를 잘못 잡을 수 있으므로 고정값을 쓴다.
 */
@Component
public class PublicUrlResolver {

    private final String baseUrl;

    public PublicUrlResolver(@Value("${app.public-base-url}") String baseUrl) {
        // 뒤 슬래시를 떼어 경로와 이을 때 "//" 가 생기지 않게 한다.
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 상대 경로 → 전체 URL. null 은 null, 이미 전체 URL이면 그대로. */
    public String absolute(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        String v = pathOrUrl.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return v;
        }
        return v.startsWith("/") ? baseUrl + v : baseUrl + "/" + v;
    }
}
