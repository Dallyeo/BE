package com.ppip.dallyeo.external.tourapi.dto;

/** detailInfo2 반복 항목 정규화 모델 (domain-entities §2). */
public record TourInfo(
        String name,  // infoname
        String text   // infotext
) {
}
