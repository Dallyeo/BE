package com.ppip.dallyeo.external.tourapi.dto;

/** detailCommon2 정규화 모델 (domain-entities §2). contentTypeId는 detailIntro2 호출·카테고리 매핑에 사용(P3). */
public record TourCommon(
        String contentId,
        String title,
        int contentTypeId,
        String overview,
        String homepage,
        String imageUrl,
        String tel,
        String address,
        Double latitude,
        Double longitude
) {
}
