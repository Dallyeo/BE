package com.ppip.dallyeo.domain.category;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void mapsKnownContentTypes() {
        assertThat(mapper.map(12, null).type()).isEqualTo(CategoryType.TOUR);
        assertThat(mapper.map(39, null).type()).isEqualTo(CategoryType.RESTAURANT);
        assertThat(mapper.map(14, null).type()).isEqualTo(CategoryType.CULTURE);
        assertThat(mapper.map(15, null).type()).isEqualTo(CategoryType.FESTIVAL);
        assertThat(mapper.map(25, null).type()).isEqualTo(CategoryType.TRAVEL_COURSE);
        assertThat(mapper.map(28, null).type()).isEqualTo(CategoryType.LEPORTS);
        assertThat(mapper.map(32, null).type()).isEqualTo(CategoryType.STAY);
        assertThat(mapper.map(38, null).type()).isEqualTo(CategoryType.SHOPPING);
    }

    @Test
    void restaurantWithFd05IsCafe() {
        assertThat(mapper.map(39, "FD05").type()).isEqualTo(CategoryType.CAFE);
        assertThat(mapper.map(39, "fd05").type()).isEqualTo(CategoryType.CAFE); // 대소문자 무시
    }

    @Test
    void unmappedFallsBackToEtcAndPreservesRaw() {
        PlaceCategory pc = mapper.map(999, null);
        assertThat(pc.type()).isEqualTo(CategoryType.ETC);
        assertThat(pc.rawContentTypeId()).isEqualTo(999);
    }

    @Test
    void alwaysPreservesRawContentTypeId() {
        assertThat(mapper.map(12, null).rawContentTypeId()).isEqualTo(12);
        assertThat(mapper.map(39, "FD05").rawContentTypeId()).isEqualTo(39);
    }
}
