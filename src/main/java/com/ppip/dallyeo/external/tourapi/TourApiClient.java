package com.ppip.dallyeo.external.tourapi;

import tools.jackson.databind.JsonNode;
import com.ppip.dallyeo.common.exception.ExternalApiException;
import com.ppip.dallyeo.common.util.LogMaskingUtil;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TourAPI 연동 Facade (US-COMMON-4, business-logic-model §3).
 *
 * <p>캐시(@Cacheable) → 회복성(@Retry+@CircuitBreaker, 엔드포인트별 인스턴스) → RestClient 호출 →
 * 정규화(TourApiNormalizer). 실패는 {@link ExternalApiException}(502)로 정규화한다.
 *
 * <p><b>serviceKey 인코딩</b>: 공공데이터포털 "Encoding" 키(%2F/%2B/%3D 포함)는 <b>재인코딩 없이 그대로</b>
 * 전송해야 한다. RestClient의 UriBuilder는 값을 재인코딩(%→%25)하거나 '+'를 공백으로 처리해 401을 유발하므로,
 * URI를 직접 조립해 serviceKey는 원본을 붙이고 나머지 값만 1회 URL-encode 한다({@link #buildUri}).
 */
@Component
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient tourApiRestClient;
    private final TourApiProperties props;
    private final TourApiNormalizer normalizer;

    public TourApiClient(RestClient tourApiRestClient, TourApiProperties props, TourApiNormalizer normalizer) {
        this.tourApiRestClient = tourApiRestClient;
        this.props = props;
        this.normalizer = normalizer;
    }

    // ===== 지역기반 목록 (areaBasedList2) =====
    @Cacheable(cacheNames = "tourApiAreaBased",
            key = "'area:' + #region.lDongRegnCd() + ':' + #region.lDongSignguCd() + ':' + #contentTypeId + ':' + #pageNo + ':' + #numOfRows")
    @Retry(name = "tourApiAreaBased", fallbackMethod = "areaBasedFallback")
    @CircuitBreaker(name = "tourApiAreaBased", fallbackMethod = "areaBasedFallback")
    public List<TourItem> areaBasedList(LDongCode region, Integer contentTypeId, int pageNo, int numOfRows) {
        Map<String, String> p = baseParams(pageNo, numOfRows);
        p.put("arrange", "A");
        p.put("lDongRegnCd", region.lDongRegnCd());
        p.put("lDongSignguCd", region.lDongSignguCd());
        putIfNotNull(p, "contentTypeId", contentTypeId);
        JsonNode root = get("/areaBasedList2", p);
        return normalizer.normalizeItems(extractItems(root), TourApiNormalizer.Operation.AREA_BASED);
    }

    @SuppressWarnings("unused")
    List<TourItem> areaBasedFallback(LDongCode region, Integer contentTypeId,
                                             int pageNo, int numOfRows, Throwable t) {
        log.warn("TourAPI areaBasedList2 fallback (regnCd={}, signguCd={}): {}",
                region.lDongRegnCd(), region.lDongSignguCd(), t.toString());
        throw new ExternalApiException("TourAPI 지역기반 목록 조회에 실패했습니다.", t);
    }

    // ===== 키워드 검색 (searchKeyword2) =====
    @Cacheable(cacheNames = "tourApiSearchKeyword",
            key = "'kw:' + #keyword + ':' + (#region == null ? 'x' : #region.lDongRegnCd() + '-' + #region.lDongSignguCd()) + ':' + #contentTypeId + ':' + #pageNo + ':' + #numOfRows")
    @Retry(name = "tourApiSearchKeyword", fallbackMethod = "searchKeywordFallback")
    @CircuitBreaker(name = "tourApiSearchKeyword", fallbackMethod = "searchKeywordFallback")
    public List<TourItem> searchKeyword(String keyword, LDongCode region, Integer contentTypeId,
                                        int pageNo, int numOfRows) {
        Map<String, String> p = baseParams(pageNo, numOfRows);
        p.put("arrange", "A");
        p.put("keyword", keyword);
        if (region != null) {
            p.put("lDongRegnCd", region.lDongRegnCd());
            p.put("lDongSignguCd", region.lDongSignguCd());
        }
        putIfNotNull(p, "contentTypeId", contentTypeId);
        JsonNode root = get("/searchKeyword2", p);
        return normalizer.normalizeItems(extractItems(root), TourApiNormalizer.Operation.SEARCH_KEYWORD);
    }

    @SuppressWarnings("unused")
    List<TourItem> searchKeywordFallback(String keyword, LDongCode region, Integer contentTypeId,
                                                 int pageNo, int numOfRows, Throwable t) {
        log.warn("TourAPI searchKeyword2 fallback (keyword={}): {}", keyword, t.toString());
        throw new ExternalApiException("TourAPI 키워드 검색에 실패했습니다.", t);
    }

    // ===== 반경 주변 (locationBasedList2) =====
    @Cacheable(cacheNames = "tourApiLocationBased",
            key = "'loc:' + #mapX + ':' + #mapY + ':' + #radius + ':' + #contentTypeId + ':' + #pageNo + ':' + #numOfRows")
    @Retry(name = "tourApiLocationBased", fallbackMethod = "locationBasedFallback")
    @CircuitBreaker(name = "tourApiLocationBased", fallbackMethod = "locationBasedFallback")
    public List<TourItem> locationBasedList(double mapX, double mapY, int radius, Integer contentTypeId,
                                            int pageNo, int numOfRows) {
        Map<String, String> p = baseParams(pageNo, numOfRows);
        p.put("arrange", "E"); // E=거리순
        p.put("mapX", String.valueOf(mapX));
        p.put("mapY", String.valueOf(mapY));
        p.put("radius", String.valueOf(radius));
        putIfNotNull(p, "contentTypeId", contentTypeId);
        JsonNode root = get("/locationBasedList2", p);
        return normalizer.normalizeItems(extractItems(root), TourApiNormalizer.Operation.LOCATION_BASED);
    }

    @SuppressWarnings("unused")
    List<TourItem> locationBasedFallback(double mapX, double mapY, int radius, Integer contentTypeId,
                                                 int pageNo, int numOfRows, Throwable t) {
        log.warn("TourAPI locationBasedList2 fallback (x={}, y={}, r={}): {}", mapX, mapY, radius, t.toString());
        throw new ExternalApiException("TourAPI 주변 장소 조회에 실패했습니다.", t);
    }

    // ===== 상세 개요 (detailCommon2) =====
    // unless: 존재하지 않는 장소는 null 반환 → 캐시하지 않음(null 캐싱 비활성과 충돌 방지, NOT_FOUND는 상위에서 404).
    @Cacheable(cacheNames = "tourApiDetailCommon", key = "'common:' + #contentId", unless = "#result == null")
    @Retry(name = "tourApiDetailCommon", fallbackMethod = "detailCommonFallback")
    @CircuitBreaker(name = "tourApiDetailCommon", fallbackMethod = "detailCommonFallback")
    public TourCommon detailCommon(String contentId) {
        Map<String, String> p = commonParams();
        p.put("contentId", contentId);
        JsonNode item = firstItem(get("/detailCommon2", p));
        return item == null ? null : normalizer.toCommon(item);
    }

    @SuppressWarnings("unused")
    TourCommon detailCommonFallback(String contentId, Throwable t) {
        log.warn("TourAPI detailCommon2 fallback (contentId={}): {}", contentId, t.toString());
        throw new ExternalApiException("TourAPI 장소 상세(개요) 조회에 실패했습니다.", t);
    }

    // ===== 상세 소개 (detailIntro2) =====
    @Cacheable(cacheNames = "tourApiDetailIntro", key = "'intro:' + #contentId + ':' + #contentTypeId")
    @Retry(name = "tourApiDetailIntro", fallbackMethod = "detailIntroFallback")
    @CircuitBreaker(name = "tourApiDetailIntro", fallbackMethod = "detailIntroFallback")
    public TourIntro detailIntro(String contentId, int contentTypeId) {
        return fetchIntro(contentId, contentTypeId);
    }

    @SuppressWarnings("unused")
    TourIntro detailIntroFallback(String contentId, int contentTypeId, Throwable t) {
        log.warn("TourAPI detailIntro2 fallback (contentId={}, typeId={}): {}", contentId, contentTypeId, t.toString());
        throw new ExternalApiException("TourAPI 장소 상세(소개) 조회에 실패했습니다.", t);
    }

    // ===== 상세 소개 — 목록 채우기 전용 (best-effort) =====

    /**
     * 목록 응답의 영업시간 채우기용 detailIntro2.
     *
     * <p>{@link #detailIntro}와 <b>캐시는 공유</b>하되(같은 cacheName/key) 회복성 인스턴스는 분리한다.
     * 목록은 한 요청에 수십~수백 건을 몰아 부르므로, 그 실패가 사용자 상세 화면({@code /places/{id}})의
     * 서킷까지 열어 버리면 안 된다. 실제로 공유 서킷으로 두면 목록 한 번에 서킷이 OPEN 되어
     * 상세 조회가 통째로 막혔다.
     *
     * <p>차이점 세 가지:
     * <ul>
     *   <li>전용 서킷 {@code tourApiDetailIntroBulk} — 상세 화면과 격리</li>
     *   <li>{@code @RateLimiter} — 공공데이터포털 <b>초당 요청 제한</b>(429 LIMITED_NUMBER…) 회피.
     *       재시도는 붙이지 않는다(부하만 2배가 되고 429를 악화시킨다)</li>
     *   <li>실패 시 예외 대신 <b>null</b> — 영업시간은 있으면 좋은 값이라 목록을 실패시키지 않는다.
     *       null은 캐시하지 않으므로({@code unless}) 다음 호출에서 다시 시도된다</li>
     * </ul>
     */
    @Cacheable(cacheNames = "tourApiDetailIntro", key = "'intro:' + #contentId + ':' + #contentTypeId",
            unless = "#result == null")
    @RateLimiter(name = "tourApiDetailIntroBulk", fallbackMethod = "detailIntroBulkFallback")
    @CircuitBreaker(name = "tourApiDetailIntroBulk", fallbackMethod = "detailIntroBulkFallback")
    public TourIntro detailIntroBulk(String contentId, int contentTypeId) {
        return fetchIntro(contentId, contentTypeId);
    }

    /** detailIntro/detailIntroBulk 공통 본문 — 차이는 회복성·캐시 정책뿐이다. */
    private TourIntro fetchIntro(String contentId, int contentTypeId) {
        Map<String, String> p = commonParams();
        p.put("contentId", contentId);
        p.put("contentTypeId", String.valueOf(contentTypeId));
        JsonNode item = firstItem(get("/detailIntro2", p));
        return item == null ? new TourIntro(null, null, null, null) : normalizer.toIntro(item, contentTypeId);
    }

    /** 목록 채우기 실패는 조용히 null — 영업시간 하나 때문에 목록이 죽으면 안 된다. */
    @SuppressWarnings("unused")
    TourIntro detailIntroBulkFallback(String contentId, int contentTypeId, Throwable t) {
        log.debug("TourAPI detailIntro2(bulk) skipped (contentId={}): {}", contentId, t.toString());
        return null;
    }

    // ===== 공통 유틸 =====

    /** 목록 오퍼레이션 공통 파라미터(페이징 포함). */
    private Map<String, String> baseParams(int pageNo, int numOfRows) {
        Map<String, String> p = commonParams();
        p.put("numOfRows", String.valueOf(numOfRows));
        p.put("pageNo", String.valueOf(pageNo));
        return p;
    }

    private Map<String, String> commonParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("MobileOS", "ETC");
        p.put("MobileApp", "Dallyeo");
        p.put("_type", "json");
        return p;
    }

    private void putIfNotNull(Map<String, String> p, String key, Object value) {
        if (value != null) {
            p.put(key, String.valueOf(value));
        }
    }

    /** URI 직접 조립 후 GET. serviceKey는 원본(Encoding) 그대로, 나머지 값만 1회 URL-encode. */
    private JsonNode get(String path, Map<String, String> params) {
        URI uri = buildUri(path, params);
        log.debug("TourAPI GET {} (serviceKey={})", path,
                LogMaskingUtil.maskServiceKey("serviceKey=" + props.serviceKey()));
        return tourApiRestClient.get().uri(uri).retrieve().body(JsonNode.class);
    }

    private URI buildUri(String path, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(props.baseUrl()).append(path)
                .append("?serviceKey=").append(props.serviceKey());   // Encoding 키 원본 유지
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            sb.append('&').append(e.getKey()).append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return URI.create(sb.toString());
    }

    /** 단일 상세 응답에서 첫 item 반환(없으면 null). */
    private JsonNode firstItem(JsonNode root) {
        List<JsonNode> items = extractItems(root);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * TourAPI 표준 응답에서 response.body.items.item[] 추출.
     * 결과 없음일 때 items가 빈 문자열/객체일 수 있어 방어적으로 처리한다.
     */
    private List<JsonNode> extractItems(JsonNode root) {
        List<JsonNode> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        JsonNode items = root.path("response").path("body").path("items");
        if (items.isMissingNode() || !items.isObject()) {
            return result;   // 결과 없음
        }
        JsonNode item = items.path("item");
        if (item.isArray()) {
            item.forEach(result::add);
        } else if (item.isObject()) {
            result.add(item);   // 단일 결과는 객체로 옴
        }
        return result;
    }
}
