package com.ppip.dallyeo.domain.category;

/**
 * 장소 카테고리 (domain-entities §3, BR-4). 미매핑/애매한 원본 타입은 ETC로 폴백한다.
 */
public enum CategoryType {
    TOUR,
    RESTAURANT,
    CAFE,
    CULTURE,
    FESTIVAL,
    TRAVEL_COURSE,
    LEPORTS,
    STAY,
    SHOPPING,
    ETC
}
