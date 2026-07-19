package com.ppip.dallyeo.domain.region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지역 정의 카탈로그 — 법정동코드(U1-a, BR-5) + 표시명(U2, BR-U2-9). 전북 시도코드 52 고정.
 * GUNSAN=(52,130,"군산"), JEONJU=(52,110,"전주").
 */
public final class RegionCatalog {

    private static final String JEONBUK_REGN_CD = "52";

    /** 순서 보존(응답 노출 순서). */
    private static final Map<Region, Definition> DEFS = new LinkedHashMap<>();

    static {
        DEFS.put(Region.GUNSAN, new Definition(new LDongCode(JEONBUK_REGN_CD, "130"), "군산"));
        DEFS.put(Region.JEONJU, new Definition(new LDongCode(JEONBUK_REGN_CD, "110"), "전주"));
    }

    private record Definition(LDongCode code, String displayName) {
    }

    private RegionCatalog() {
    }

    /** 미등록 지역이면 null. 예외 변환은 RegionCodeMapper가 담당. */
    public static LDongCode find(Region region) {
        Definition d = region == null ? null : DEFS.get(region);
        return d == null ? null : d.code();
    }

    /** 한글 표시명(군산/전주). 미등록이면 null. */
    public static String displayName(Region region) {
        Definition d = region == null ? null : DEFS.get(region);
        return d == null ? null : d.displayName();
    }

    /** 지원 지역(정의 순서). */
    public static List<Region> supportedRegions() {
        return List.copyOf(DEFS.keySet());
    }

    /** 한글 지역명("군산"/"전주") → Region. 미등록이면 null(적재 스킵 판단용, BR-U2-2). */
    public static Region fromDisplayName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        for (Map.Entry<Region, Definition> e : DEFS.entrySet()) {
            if (e.getValue().displayName().equals(trimmed)) {
                return e.getKey();
            }
        }
        return null;
    }
}
