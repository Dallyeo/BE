package com.ppip.dallyeo.domain.region;

import java.util.Map;

/**
 * 지역 → 법정동코드 상수 카탈로그 (BR-5, F5). 전북 시도코드 52 고정.
 * GUNSAN=(52,130), JEONJU=(52,110).
 */
public final class RegionCatalog {

    private static final String JEONBUK_REGN_CD = "52";

    private static final Map<Region, LDongCode> CODES = Map.of(
            Region.GUNSAN, new LDongCode(JEONBUK_REGN_CD, "130"),
            Region.JEONJU, new LDongCode(JEONBUK_REGN_CD, "110")
    );

    private RegionCatalog() {
    }

    /** 미등록 지역이면 null. 예외 변환은 RegionCodeMapper가 담당. */
    public static LDongCode find(Region region) {
        return region == null ? null : CODES.get(region);
    }
}
