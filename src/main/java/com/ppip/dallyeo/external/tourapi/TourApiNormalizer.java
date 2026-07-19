package com.ppip.dallyeo.external.tourapi;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import com.ppip.dallyeo.external.tourapi.dto.TourItem;

/**
 * TourAPI 원본 JSON → 내부 정규화 모델 변환 (BR-3).
 * - 빈 문자열 "" → null (BR-3.1)
 * - mapx(경도)/mapy(위도)/dist(거리) 문자열 → double (BR-3.2, X=경도/Y=위도 주의)
 * - 좌표 파싱 실패 시 엔드포인트별 정책 (BR-3.3): location=제외, area/keyword=null 유지
 * - 제외/실패 카운트 로그 (BR-3.4)
 */
@Component
public class TourApiNormalizer {

    private static final Logger log = LoggerFactory.getLogger(TourApiNormalizer.class);

    /** TourAPI 오퍼레이션별 좌표 정책. */
    public enum Operation {
        AREA_BASED(false),
        LOCATION_BASED(true),   // 반경 검색 → 좌표 필수
        SEARCH_KEYWORD(false);

        private final boolean coordinateRequired;

        Operation(boolean coordinateRequired) {
            this.coordinateRequired = coordinateRequired;
        }
    }

    /** 빈 문자열/누락 → null (BR-3.1). */
    public String nullIfBlank(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 좌표/거리 문자열 → Double. 파싱 불가/빈값 → null (BR-3.2). */
    public Double parseCoordinate(String v) {
        String s = nullIfBlank(v);
        if (s == null) {
            return null;
        }
        try {
            return Double.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 목록 응답 항목들을 정규화한다. 엔드포인트별 좌표 정책과 제외 카운트 로깅을 적용한다(BR-3.3/3.4).
     */
    public List<TourItem> normalizeItems(List<JsonNode> rawItems, Operation op) {
        List<TourItem> result = new ArrayList<>();
        int excluded = 0;
        for (JsonNode raw : rawItems) {
            Double lat = parseCoordinate(text(raw, "mapy"));
            Double lng = parseCoordinate(text(raw, "mapx"));

            if (op.coordinateRequired && (lat == null || lng == null)) {
                excluded++;   // BR-3.3: location 반경 → 좌표 실패 항목 제외
                continue;
            }
            result.add(toItem(raw, lat, lng));
        }
        if (excluded > 0) {
            // BR-3.4: 제외 카운트 로그(운영 가시성)
            log.info("TourAPI normalize op={} excluded {} item(s) due to missing/invalid coordinates", op, excluded);
        }
        return result;
    }

    private TourItem toItem(JsonNode raw, Double lat, Double lng) {
        String addr1 = nullIfBlank(text(raw, "addr1"));
        String addr2 = nullIfBlank(text(raw, "addr2"));
        String address = addr2 == null ? addr1 : (addr1 == null ? addr2 : addr1 + " " + addr2);
        return new TourItem(
                nullIfBlank(text(raw, "contentid")),
                nullIfBlank(text(raw, "title")),
                lat,
                lng,
                parseCoordinate(text(raw, "dist")),
                address,
                nullIfBlank(text(raw, "firstimage2")),
                parseInt(text(raw, "contenttypeid")),
                nullIfBlank(text(raw, "lclsSystm1")),
                nullIfBlank(text(raw, "lclsSystm2")),
                nullIfBlank(text(raw, "lclsSystm3"))
        );
    }

    private int parseInt(String v) {
        String s = nullIfBlank(v);
        if (s == null) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
