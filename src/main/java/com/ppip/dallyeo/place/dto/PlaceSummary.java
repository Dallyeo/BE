package com.ppip.dallyeo.place.dto;

import com.ppip.dallyeo.domain.category.CategoryType;

import java.util.List;

/**
 * 장소 목록 항목 (US-PLACE-1/2/3). distanceMeters는 nearby 전용(그 외 null).
 * badges는 상세({@code PlaceDetail})와 동일 형태 — 미매칭이면 빈 배열.
 */
public record PlaceSummary(
        String id,
        String name,
        CategoryType category,
        Double latitude,
        Double longitude,
        String address,
        String thumbnailUrl,
        Double distanceMeters,
        List<String> badges
) {

    /** 배지만 교체한 사본. 매핑(TourAPI 변환)과 배지 부착(DB 조회)을 분리하기 위한 것. */
    public PlaceSummary withBadges(List<String> badges) {
        return new PlaceSummary(id, name, category, latitude, longitude,
                address, thumbnailUrl, distanceMeters, badges);
    }
}
