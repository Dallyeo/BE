package com.ppip.dallyeo.external.tourapi;

import tools.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ppip.dallyeo.common.util.BusinessHoursText;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;

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

    // ===== U3: 상세(detail) 파싱 =====

    private static final Pattern HREF = Pattern.compile("href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    /** detailCommon2 항목 → TourCommon (개요/홈페이지/이미지/좌표). */
    public TourCommon toCommon(JsonNode raw) {
        String addr1 = nullIfBlank(text(raw, "addr1"));
        String addr2 = nullIfBlank(text(raw, "addr2"));
        String address = addr2 == null ? addr1 : (addr1 == null ? addr2 : addr1 + " " + addr2);
        return new TourCommon(
                nullIfBlank(text(raw, "contentid")),
                nullIfBlank(text(raw, "title")),
                parseInt(text(raw, "contenttypeid")),
                nullIfBlank(text(raw, "overview")),
                extractHref(nullIfBlank(text(raw, "homepage"))),
                nullIfBlank(text(raw, "firstimage")),
                nullIfBlank(text(raw, "tel")),
                address,
                parseCoordinate(text(raw, "mapy")),
                parseCoordinate(text(raw, "mapx"))
        );
    }

    /**
     * detailIntro2 항목 → TourIntro. contentTypeId별로 필드명이 전부 다르다(BR-U3-6).
     * 목록 응답에도 businessHours를 싣게 되면서 관광지/음식점 외 타입도 매핑한다
     * (미매핑이면 목록 카드 둘째 줄이 타입별로 비어 버린다).
     *
     * <p>businessHours는 {@link BusinessHoursText#normalize} 로 정리해 <b>개행 구분</b>으로 돌려준다.
     * 숙박(32)은 영업시간 개념이 없어 체크인/체크아웃을 대신 싣는다.
     */
    public TourIntro toIntro(JsonNode raw, int contentTypeId) {
        return switch (contentTypeId) {
            case 12 -> intro(raw, "usetime", "restdate", "parking", "infocenter");
            case 14 -> intro(raw, "usetimeculture", "restdateculture", "parkingculture", "infocenterculture");
            case 15 -> intro(raw, "playtime", null, "parkingfestival", "sponsor1tel");
            case 25 -> intro(raw, null, null, null, "infocentertourcourse");
            case 28 -> intro(raw, "usetimeleports", "restdateleports", "parkingleports", "infocenterleports");
            case 32 -> new TourIntro(
                    stayHours(raw),
                    nullIfBlank(text(raw, "restdate")),
                    nullIfBlank(text(raw, "parkinglodging")),
                    nullIfBlank(text(raw, "infocenterlodging")));
            case 38 -> intro(raw, "opentime", "restdateshopping", "parkingshopping", "infocentershopping");
            case 39 -> intro(raw, "opentimefood", "restdatefood", "parkingfood", "infocenterfood");
            default -> {
                // BR-U3-6: 미매핑 타입 → businessHours 등 null + WARN(신규 타입 인지)
                log.warn("TourAPI detailIntro2 unmapped contentTypeId={} -> businessHours null", contentTypeId);
                yield new TourIntro(null, null, null, null);
            }
        };
    }

    /** 타입별 필드명만 갈아끼우는 공통 조립. 필드명이 null이면 해당 값도 null. */
    private TourIntro intro(JsonNode raw, String hoursField, String restField,
                            String parkingField, String inquiryField) {
        return new TourIntro(
                BusinessHoursText.normalize(field(raw, hoursField)),
                nullIfBlank(field(raw, restField)),
                nullIfBlank(field(raw, parkingField)),
                nullIfBlank(field(raw, inquiryField)));
    }

    /** 숙박(32): 영업시간 대신 체크인/체크아웃. 둘 다 없으면 null. */
    private String stayHours(JsonNode raw) {
        String in = nullIfBlank(text(raw, "checkintime"));
        String out = nullIfBlank(text(raw, "checkouttime"));
        if (in == null && out == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            sb.append("체크인 ").append(in);
        }
        if (out != null) {
            sb.append(sb.length() > 0 ? " / " : "").append("체크아웃 ").append(out);
        }
        return BusinessHoursText.normalize(sb.toString());
    }

    private String field(JsonNode raw, String name) {
        return name == null ? null : text(raw, name);
    }

    /** homepage 필드의 &lt;a href="..."&gt; 에서 URL만 추출. 태그 없으면 원문 반환. */
    private String extractHref(String homepage) {
        if (homepage == null) {
            return null;
        }
        Matcher m = HREF.matcher(homepage);
        return m.find() ? m.group(1) : homepage;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }
}
