package com.ppip.dallyeo.external.tourapi.dto;

/** detailCommon2 정규화 모델 (domain-entities §2). */
public record TourCommon(
        String contentId,
        String title,
        String overview,
        String homepage,
        String imageUrl,
        String tel,
        String address,
        Double latitude,
        Double longitude
) {
}
