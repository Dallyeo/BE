package com.ppip.dallyeo.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이미지 URL을 전체 URL로 통일하는 규칙.
 * 요점은 <b>이미 전체 URL인 값(TourAPI 장소 이미지)을 건드리지 않는 것</b> —
 * 그래야 모든 이미지 필드에 안전하게 적용할 수 있다.
 */
class PublicUrlResolverTest {

    private final PublicUrlResolver resolver = new PublicUrlResolver("https://dallyeo.cloud");

    @Test
    void prependsBaseUrlToRelativePath() {
        assertThat(resolver.absolute("/images/courses/gunsan-jjamppong-run.png"))
                .isEqualTo("https://dallyeo.cloud/images/courses/gunsan-jjamppong-run.png");
        assertThat(resolver.absolute("/uploads/runs/abc.jpg"))
                .isEqualTo("https://dallyeo.cloud/uploads/runs/abc.jpg");
    }

    @Test
    void leavesAbsoluteUrlUntouched() {
        // TourAPI 장소 이미지 — 도메인을 또 붙이면 깨진다.
        String tourApi = "http://tong.visitkorea.or.kr/cms/resource/14/3055614_image3_1.jpg";
        assertThat(resolver.absolute(tourApi)).isEqualTo(tourApi);
        assertThat(resolver.absolute("https://example.com/a.png")).isEqualTo("https://example.com/a.png");
    }

    @Test
    void nullAndBlankStayNull() {
        assertThat(resolver.absolute(null)).isNull();
        assertThat(resolver.absolute("  ")).isNull();
    }

    @Test
    void doesNotProduceDoubleSlash() {
        assertThat(new PublicUrlResolver("https://dallyeo.cloud/").absolute("/images/a.png"))
                .isEqualTo("https://dallyeo.cloud/images/a.png");
    }

    @Test
    void addsSlashWhenPathHasNone() {
        assertThat(resolver.absolute("images/a.png")).isEqualTo("https://dallyeo.cloud/images/a.png");
    }
}
