package com.ppip.dallyeo.place.dto;

import com.ppip.dallyeo.domain.category.CategoryType;

import java.util.List;

/**
 * 장소 상세 (US-PLACE-4). detailCommon2+detailIntro2 조합 + 배지.
 */
public record PlaceDetail(
        String id,
        String name,
        CategoryType category,
        Double latitude,
        Double longitude,
        String address,
        String businessHours,
        String imageUrl,
        List<String> badges
) {
}
