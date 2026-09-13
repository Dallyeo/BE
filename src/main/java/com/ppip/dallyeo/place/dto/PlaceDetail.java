package com.ppip.dallyeo.place.dto;

import com.ppip.dallyeo.domain.category.CategoryType;

import java.util.List;

/**
 * 장소 상세 (US-PLACE-4). detailCommon2+detailIntro2 조합 + 배지.
 *
 * <p>businessHours는 TourAPI 원문을 정리해 개행(\n)으로 구분한 전체 항목,
 * openHours는 그중 첫 항목(카드/헤더 한 줄용 대표값)이다.
 */
public record PlaceDetail(
        String id,
        String name,
        CategoryType category,
        Double latitude,
        Double longitude,
        String address,
        String businessHours,
        String openHours,
        String imageUrl,
        List<String> badges
) {
}
