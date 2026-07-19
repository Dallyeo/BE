package com.ppip.dallyeo.place.dto;

import com.ppip.dallyeo.domain.category.CategoryType;

/**
 * 장소 목록 항목 (US-PLACE-1/2/3). distanceMeters는 nearby 전용(그 외 null).
 */
public record PlaceSummary(
        String id,
        String name,
        CategoryType category,
        Double latitude,
        Double longitude,
        String address,
        String thumbnailUrl,
        Double distanceMeters
) {
}
