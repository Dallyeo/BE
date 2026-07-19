package com.ppip.dallyeo.external.tourapi.dto;

/**
 * TourAPI 목록 항목 정규화 모델 (area/location/keyword 공통, domain-entities §2).
 * 좌표는 area/keyword에서 null 가능, location에서는 필수(BR-3.3).
 */
public record TourItem(
        String contentId,
        String title,
        Double latitude,      // mapy
        Double longitude,     // mapx
        Double distanceMeters,// dist (location 전용)
        String address,       // addr1(+addr2)
        String thumbnailUrl,  // firstimage2
        int contentTypeId,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3
) {
}
