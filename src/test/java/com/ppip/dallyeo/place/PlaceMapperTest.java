package com.ppip.dallyeo.place;

import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceDetail;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceMapperTest {

    private final PlaceMapper mapper = new PlaceMapper(new CategoryMapper());

    private TourItem item(int typeId, String lcls2) {
        return new TourItem("1", "가게", 35.9, 126.7, 120.0, "전북 군산시", "http://img",
                typeId, "A", lcls2, "C");
    }

    @Test
    void toSummary_mapsCategoryAndFields() {
        PlaceSummary s = mapper.toSummary(item(39, null));
        assertThat(s.category()).isEqualTo(CategoryType.RESTAURANT);
        assertThat(s.latitude()).isEqualTo(35.9);
        assertThat(s.distanceMeters()).isEqualTo(120.0);
    }

    @Test
    void toSummary_cafeViaLclsSystm2() {
        assertThat(mapper.toSummary(item(39, "FD05")).category()).isEqualTo(CategoryType.CAFE);
    }

    @Test
    void toDetail_composesCommonIntroBadges() {
        TourCommon common = new TourCommon("1", "맛집", 39, "개요", "http://hp", "http://img",
                "063-000", "전북 군산시", 35.9, 126.7);
        TourIntro intro = new TourIntro("11:30~19:00", "화요일", null, null);

        PlaceDetail d = mapper.toDetail(common, intro, 39, List.of("MODEL_RESTAURANT"));

        assertThat(d.category()).isEqualTo(CategoryType.RESTAURANT);
        assertThat(d.businessHours()).isEqualTo("11:30~19:00");
        assertThat(d.imageUrl()).isEqualTo("http://img");
        assertThat(d.badges()).containsExactly("MODEL_RESTAURANT");
    }
}
