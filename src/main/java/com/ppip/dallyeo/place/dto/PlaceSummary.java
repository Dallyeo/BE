package com.ppip.dallyeo.place.dto;

import com.ppip.dallyeo.domain.category.CategoryType;

import java.util.List;

/**
 * 장소 목록 항목 (US-PLACE-1/2/3). distanceMeters는 nearby 전용(그 외 null).
 * badges는 상세({@code PlaceDetail})와 동일 형태 — 미매칭이면 빈 배열.
 *
 * <p>businessHours/openHours도 상세와 동일 형태다. 검색 결과 카드 둘째 줄이
 * "거리 · 영업시간"이라 목록에서 바로 필요하기 때문. businessHours는 개행(\n) 구분 전체 항목,
 * openHours는 그중 첫 항목(카드 한 줄용 대표값)이다. 정보가 없으면 둘 다 null.
 */
public record PlaceSummary(
        String id,
        String name,
        CategoryType category,
        Double latitude,
        Double longitude,
        String address,
        String businessHours,
        String openHours,
        String thumbnailUrl,
        Double distanceMeters,
        List<String> badges
) {

    /** 배지만 교체한 사본. 매핑(TourAPI 변환)과 배지 부착(DB 조회)을 분리하기 위한 것. */
    public PlaceSummary withBadges(List<String> badges) {
        return new PlaceSummary(id, name, category, latitude, longitude, address,
                businessHours, openHours, thumbnailUrl, distanceMeters, badges);
    }

    /** 영업시간만 교체한 사본. 목록 매핑 이후 detailIntro2로 뒤늦게 채우기 위한 것. */
    public PlaceSummary withBusinessHours(String businessHours, String openHours) {
        return new PlaceSummary(id, name, category, latitude, longitude, address,
                businessHours, openHours, thumbnailUrl, distanceMeters, badges);
    }
}
