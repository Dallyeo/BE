package com.ppip.dallyeo.domain.category;

/**
 * 카테고리 값 객체 (BR-4.3). 매핑 결과 {@link CategoryType}과 함께
 * 원본 contentTypeId를 보존한다(폴백 포함) — 신규 타입 등장 추적 목적.
 */
public record PlaceCategory(CategoryType type, int rawContentTypeId) {
}
