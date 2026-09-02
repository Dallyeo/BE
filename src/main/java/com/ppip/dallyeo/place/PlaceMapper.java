package com.ppip.dallyeo.place;

import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceDetail;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TourAPI DTO → Place DTO 변환 (BR-U3-1). 카테고리는 U1-a CategoryMapper 재사용.
 * businessHours 타입 분기/null 사유 구분은 TourApiNormalizer.toIntro가 처리(BR-U3-6).
 */
@Component
public class PlaceMapper {

    private final CategoryMapper categoryMapper;

    public PlaceMapper(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /** 배지는 여기서 채우지 않는다(빈 배열) — 목록은 PlaceService가 일괄 조회로 부착한다. */
    public PlaceSummary toSummary(TourItem item) {
        CategoryType category = categoryMapper.map(item.contentTypeId(), item.lclsSystm2()).type();
        return new PlaceSummary(
                item.contentId(), item.title(), category,
                item.latitude(), item.longitude(), item.address(),
                item.thumbnailUrl(), item.distanceMeters(), List.of());
    }

    public List<PlaceSummary> toSummaries(List<TourItem> items) {
        return items.stream().map(this::toSummary).toList();
    }

    /**
     * 상세 조합. category는 detailCommon2에서 확보한 contentTypeId로 매핑(assembler 제공),
     * businessHours는 intro(타입 분기 결과), badges는 BadgeService 결과.
     */
    public PlaceDetail toDetail(TourCommon common, TourIntro intro, int contentTypeId, List<String> badges) {
        CategoryType category = categoryMapper.map(contentTypeId, null).type();
        return new PlaceDetail(
                common.contentId(), common.title(), category,
                common.latitude(), common.longitude(), common.address(),
                intro == null ? null : intro.businessHours(),
                common.imageUrl(), badges);
    }
}
