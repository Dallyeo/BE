package com.ppip.dallyeo.external.tourapi.dto;

/**
 * detailIntro2 정규화 모델 (domain-entities §2).
 * 타입별 원본 필드(관광지/음식점 등)는 매핑 단계에서 공통 필드로 흡수.
 */
public record TourIntro(
        String businessHours,
        String restDate,
        String parking,
        String inquiry
) {
}
