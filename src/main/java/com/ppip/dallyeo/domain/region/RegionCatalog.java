package com.ppip.dallyeo.domain.region;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 지역 정의 카탈로그 — 법정동코드(U1-a, BR-5) + 표시명(U2, BR-U2-9). 전북 시도코드 52 고정.
 *
 * <p><b>지역 하나가 법정동코드를 여러 개 가질 수 있다.</b> 전주가 그렇다 —
 * TourAPI에는 전주시(110) 자체 데이터가 <b>0건</b>이고 실제 데이터는 자치구 단위
 * 완산구(111)·덕진구(113)에 들어 있다. 110만 쓰던 동안 전주 지도·검색이 통째로 빈 화면이었다.
 */
public final class RegionCatalog {

    private static final String JEONBUK_REGN_CD = "52";

    /** 순서 보존(응답 노출 순서). */
    private static final Map<Region, Definition> DEFS = new LinkedHashMap<>();

    static {
        DEFS.put(Region.GUNSAN, new Definition(codes("130"), "군산"));
        // 전주시(110)는 TourAPI 등록 데이터 0건 — 자치구 단위로 조회해 합쳐야 한다.
        DEFS.put(Region.JEONJU, new Definition(codes("111", "113"), "전주"));
    }

    private static List<LDongCode> codes(String... signguCds) {
        return Arrays.stream(signguCds)
                .map(cd -> new LDongCode(JEONBUK_REGN_CD, cd))
                .toList();
    }

    private record Definition(List<LDongCode> codes, String displayName) {
    }

    private RegionCatalog() {
    }

    /**
     * 해당 지역을 조회하는 데 필요한 법정동코드 전부. 미등록 지역이면 빈 목록.
     * 예외 변환은 RegionCodeMapper가 담당.
     */
    public static List<LDongCode> find(Region region) {
        Definition d = region == null ? null : DEFS.get(region);
        return d == null ? List.of() : d.codes();
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
