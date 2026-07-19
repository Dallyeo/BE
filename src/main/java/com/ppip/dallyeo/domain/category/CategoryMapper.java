package com.ppip.dallyeo.domain.category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * contentTypeId(+lclsSystm2) → {@link CategoryType} 매핑 (BR-4).
 * 미매핑/애매 → ETC 폴백 + 원본 보존 + WARN 로그(조용한 폴백 금지, BR-4.4).
 */
@Component
public class CategoryMapper {

    private static final Logger log = LoggerFactory.getLogger(CategoryMapper.class);

    /** 음식점(39) 중 카페 분류용 대분류 코드. */
    private static final String LCLS_CAFE = "FD05";

    /**
     * @param contentTypeId TourAPI 콘텐츠 타입 ID
     * @param lclsSystm2    분류체계 2단계(카페 구분용, null 허용)
     */
    public PlaceCategory map(int contentTypeId, String lclsSystm2) {
        CategoryType type = switch (contentTypeId) {
            case 12 -> CategoryType.TOUR;
            case 39 -> LCLS_CAFE.equalsIgnoreCase(lclsSystm2) ? CategoryType.CAFE : CategoryType.RESTAURANT;
            case 14 -> CategoryType.CULTURE;
            case 15 -> CategoryType.FESTIVAL;
            case 25 -> CategoryType.TRAVEL_COURSE;
            case 28 -> CategoryType.LEPORTS;
            case 32 -> CategoryType.STAY;
            case 38 -> CategoryType.SHOPPING;
            default -> null;
        };

        if (type == null) {
            // BR-4.2 / BR-4.4: ETC 폴백 + WARN(원본 typeId 포함)
            log.warn("Unmapped TourAPI contentTypeId={} (lclsSystm2={}) -> fallback ETC", contentTypeId, lclsSystm2);
            type = CategoryType.ETC;
        }
        return new PlaceCategory(type, contentTypeId);
    }
}
