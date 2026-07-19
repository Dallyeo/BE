package com.ppip.dallyeo.external.tourapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourApiNormalizerTest {

    private final TourApiNormalizer normalizer = new TourApiNormalizer();
    private final ObjectMapper om = new ObjectMapper();

    private JsonNode item(String json) throws Exception {
        return om.readTree(json);
    }

    @Test
    void blankToNull() {
        assertThat(normalizer.nullIfBlank("")).isNull();
        assertThat(normalizer.nullIfBlank("  ")).isNull();
        assertThat(normalizer.nullIfBlank(null)).isNull();
        assertThat(normalizer.nullIfBlank(" x ")).isEqualTo("x");
    }

    @Test
    void parseCoordinate() {
        assertThat(normalizer.parseCoordinate("126.9")).isEqualTo(126.9);
        assertThat(normalizer.parseCoordinate("")).isNull();
        assertThat(normalizer.parseCoordinate("not-a-num")).isNull();
    }

    @Test
    void areaBased_keepsItemsWithNullCoordinates() throws Exception {
        List<JsonNode> raw = List.of(
                item("{\"contentid\":\"1\",\"title\":\"A\",\"mapx\":\"127.0\",\"mapy\":\"35.8\",\"contenttypeid\":\"12\"}"),
                item("{\"contentid\":\"2\",\"title\":\"B\",\"mapx\":\"\",\"mapy\":\"\",\"contenttypeid\":\"39\"}")
        );
        List<TourItem> items = normalizer.normalizeItems(raw, TourApiNormalizer.Operation.AREA_BASED);

        assertThat(items).hasSize(2); // 좌표 없어도 보존
        assertThat(items.get(0).longitude()).isEqualTo(127.0);
        assertThat(items.get(0).latitude()).isEqualTo(35.8);
        assertThat(items.get(1).longitude()).isNull();
        assertThat(items.get(1).latitude()).isNull();
    }

    @Test
    void locationBased_excludesItemsWithInvalidCoordinates() throws Exception {
        List<JsonNode> raw = List.of(
                item("{\"contentid\":\"1\",\"title\":\"A\",\"mapx\":\"127.0\",\"mapy\":\"35.8\",\"dist\":\"120.5\"}"),
                item("{\"contentid\":\"2\",\"title\":\"B\",\"mapx\":\"\",\"mapy\":\"\"}")
        );
        List<TourItem> items = normalizer.normalizeItems(raw, TourApiNormalizer.Operation.LOCATION_BASED);

        assertThat(items).hasSize(1); // 좌표 없는 항목 제외
        assertThat(items.get(0).contentId()).isEqualTo("1");
        assertThat(items.get(0).distanceMeters()).isEqualTo(120.5);
    }

    @Test
    void concatenatesAddress() throws Exception {
        List<JsonNode> raw = List.of(
                item("{\"contentid\":\"1\",\"addr1\":\"전북 군산시\",\"addr2\":\"중앙로 1\",\"mapx\":\"127\",\"mapy\":\"35\"}")
        );
        TourItem it = normalizer.normalizeItems(raw, TourApiNormalizer.Operation.AREA_BASED).get(0);
        assertThat(it.address()).isEqualTo("전북 군산시 중앙로 1");
    }
}
