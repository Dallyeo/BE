package com.ppip.dallyeo.badge;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressNormalizerTest {

    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void unifiesProvinceAndStripsWhitespace() {
        String a = normalizer.normalizeAddress("전북특별자치도 군산시 경암5길 63");
        String b = normalizer.normalizeAddress("전라북도 군산시 경암5길 63");
        String c = normalizer.normalizeAddress("전북 군산시 경암5길 63");
        assertThat(a).isEqualTo(b).isEqualTo(c);   // 시도명 표기 통일
        assertThat(a).isEqualTo("전북군산시경암5길63");
    }

    @Test
    void removesParenthesesContent() {
        assertThat(normalizer.normalizeAddress("전북 군산시 대학로 649 (신관동)"))
                .isEqualTo("전북군산시대학로649");
    }

    @Test
    void normalizesNameRemovingSpecialChars() {
        assertThat(normalizer.normalizeName("바다 회집 (현대코아점)")).isEqualTo("바다회집");
        assertThat(normalizer.normalizeName("A-1 카페.")).isEqualTo("a1카페");
    }

    @Test
    void removesBracketedTags() {
        // TourAPI가 상호에 붙이는 [착한가게]/[모범음식점] 태그 제거 → CSV 상호와 매칭
        assertThat(normalizer.normalizeName("[착한가게] 아서원")).isEqualTo("아서원");
        assertThat(normalizer.normalizeName("[모범음식점] 빈해원")).isEqualTo("빈해원");
    }

    @Test
    void nullSafe() {
        assertThat(normalizer.normalizeName(null)).isEmpty();
        assertThat(normalizer.normalizeAddress(null)).isEmpty();
    }
}
