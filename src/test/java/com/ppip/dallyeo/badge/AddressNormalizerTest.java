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

    // ===== 상세주소(층/호) 절단 — 공공데이터 CSV ↔ TourAPI 표기 차이 흡수 =====

    @Test
    void cutsDetailAddressAfterComma() {
        // CSV "…, 1층" 과 TourAPI "…" 가 같은 키로 정규화되어야 매칭된다.
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 수송남로 2, 1층"))
                .isEqualTo(normalizer.normalizeAddress("전북특별자치도 군산시 수송남로 2"))
                .isEqualTo("전북군산시수송남로2");
    }

    @Test
    void cutsUnitNumbersAfterComma() {
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 수송로 315, 101호,102호 (미장동)"))
                .isEqualTo("전북군산시수송로315");
    }

    @Test
    void handlesParenthesizedFloorWithoutEatingBuildingNumber() {
        // "(1,2)층" 을 일반 괄호 제거로 먼저 지우면 "15 층"이 남아 건물번호까지 깎이는 함정.
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 하나운1길 15 (1,2)층 (나운동)"))
                .isEqualTo(normalizer.normalizeAddress("전북특별자치도 군산시 하나운1길 15 (나운동)"))
                .isEqualTo("전북군산시하나운1길15");
    }

    @Test
    void keepsBuildingNumberWithHyphenAndFloorSuffix() {
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 은파순환길 174-4,2층(미룡동)"))
                .isEqualTo("전북군산시은파순환길1744");
    }

    @Test
    void handlesNestedParenthesesInDongField() {
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 백토로 284-8 (나운동, (1층, 2층))"))
                .isEqualTo("전북군산시백토로2848");
    }

    @Test
    void differentBuildingNumbersStayDistinct() {
        // 상세주소만 잘라낼 뿐 도로명+건물번호는 그대로 비교 → 다른 건물이 뭉치지 않는다.
        assertThat(normalizer.normalizeAddress("전북 군산시 수송로 315, 1층"))
                .isNotEqualTo(normalizer.normalizeAddress("전북 군산시 수송로 316, 1층"));
    }

    @Test
    void addressWithoutCommaIsUnchanged() {
        assertThat(normalizer.normalizeAddress("전북특별자치도 군산시 경암5길 63"))
                .isEqualTo("전북군산시경암5길63");
    }
}
