package com.ppip.dallyeo.external.tourapi.dto;

/** detailImage2 정규화 모델 (domain-entities §2). */
public record TourImage(
        String url,          // originimgurl
        String thumbnailUrl, // smallimageurl
        String name          // imgname
) {
}
