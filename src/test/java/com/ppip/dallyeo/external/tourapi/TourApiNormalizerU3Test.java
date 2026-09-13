package com.ppip.dallyeo.external.tourapi;

import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiNormalizerU3Test {

    private final TourApiNormalizer normalizer = new TourApiNormalizer();
    private final ObjectMapper om = new ObjectMapper();

    private JsonNode node(String json) {
        return om.readTree(json);
    }

    @Test
    void toCommon_parsesFieldsAndHref() {
        JsonNode raw = node("""
                {"contentid":"123","title":"군산 장소","contenttypeid":"12",
                 "overview":"소개","homepage":"<a href=\\"http://x.com\\">홈</a>",
                 "firstimage":"http://img","tel":"","addr1":"전북 군산시","addr2":"1길",
                 "mapx":"126.7","mapy":"35.9"}
                """);
        TourCommon c = normalizer.toCommon(raw);

        assertThat(c.contentId()).isEqualTo("123");
        assertThat(c.contentTypeId()).isEqualTo(12);
        assertThat(c.homepage()).isEqualTo("http://x.com");   // href만 추출
        assertThat(c.tel()).isNull();                          // "" → null
        assertThat(c.address()).isEqualTo("전북 군산시 1길");
        assertThat(c.latitude()).isEqualTo(35.9);
    }

    @Test
    void toIntro_tourType12_usesUsetime() {
        JsonNode raw = node("{\"usetime\":\"09:00-18:00\",\"restdate\":\"연중무휴\"}");
        TourIntro intro = normalizer.toIntro(raw, 12);
        assertThat(intro.businessHours()).isEqualTo("09:00-18:00");
        assertThat(intro.restDate()).isEqualTo("연중무휴");
    }

    @Test
    void toIntro_restaurantType39_usesOpentimefood() {
        JsonNode raw = node("{\"opentimefood\":\"11:30~19:00\",\"restdatefood\":\"매주 화요일\"}");
        TourIntro intro = normalizer.toIntro(raw, 39);
        assertThat(intro.businessHours()).isEqualTo("11:30~19:00");
    }

    @Test
    void toIntro_unmappedType_returnsNulls() {
        JsonNode raw = node("{\"usetime\":\"x\"}");
        TourIntro intro = normalizer.toIntro(raw, 99);   // 미매핑 타입
        assertThat(intro.businessHours()).isNull();
    }
    @Test
    void toIntro_restaurant_brTagsBecomeNewlines() {
        // 등대로(1305903) 실제 형태: 항목 구분이 <br>뿐이라 태그만 지우면 한 줄로 뭉개진다.
        JsonNode raw = node("{\"opentimefood\":\"- 12:00~21:00<br>- 준비시간 14:00~17:00<br />- 마지막 주문 20:30\"}");

        TourIntro intro = normalizer.toIntro(raw, 39);

        assertThat(intro.businessHours())
                .isEqualTo("12:00~21:00\n준비시간 14:00~17:00\n마지막 주문 20:30");
    }

    @Test
    void toIntro_cultureType14_usesUsetimeculture() {
        JsonNode raw = node("{\"usetimeculture\":\"09:00~18:00\",\"restdateculture\":\"월요일\"}");
        TourIntro intro = normalizer.toIntro(raw, 14);
        assertThat(intro.businessHours()).isEqualTo("09:00~18:00");
        assertThat(intro.restDate()).isEqualTo("월요일");
    }

    @Test
    void toIntro_shoppingType38_usesOpentime() {
        JsonNode raw = node("{\"opentime\":\"10:00~20:00\"}");
        assertThat(normalizer.toIntro(raw, 38).businessHours()).isEqualTo("10:00~20:00");
    }

    @Test
    void toIntro_stayType32_usesCheckInOut() {
        JsonNode raw = node("{\"checkintime\":\"15:00\",\"checkouttime\":\"11:00\"}");
        assertThat(normalizer.toIntro(raw, 32).businessHours()).isEqualTo("체크인 15:00 / 체크아웃 11:00");
    }
}
