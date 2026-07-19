package com.ppip.dallyeo.external.tourapi;

import tools.jackson.databind.JsonNode;
import com.ppip.dallyeo.common.exception.ExternalApiException;
import com.ppip.dallyeo.common.util.LogMaskingUtil;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * TourAPI 연동 Facade (US-COMMON-4, business-logic-model §3).
 *
 * <p>U1-a는 대표 오퍼레이션 {@code areaBasedList2} 하나로 계약을 검증하는 <b>골격</b>이다:
 * 캐시(@Cacheable, D3/BR-6) → 회복성(@Retry+@CircuitBreaker, D1/D2/BR-7) → RestClient 호출 →
 * 정규화(TourApiNormalizer, BR-3). 실패는 {@link ExternalApiException}(502)로 정규화한다.
 *
 * <p>location/keyword/detail* 오퍼레이션은 동일 패턴으로 U2/U3에서 추가한다(엔드포인트별 서킷 인스턴스).
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

    /**
     * 지역기반 관광정보 목록 조회 (areaBasedList2).
     *
     * @param region        법정동 코드(지역)
     * @param contentTypeId 콘텐츠 타입 필터(null 허용)
     */
    @Cacheable(cacheNames = "tourApiAreaBased",
            key = "'area:' + #region.lDongRegnCd() + ':' + #region.lDongSignguCd() + ':' + #contentTypeId + ':' + #pageNo + ':' + #numOfRows")
    @Retry(name = "tourApiAreaBased", fallbackMethod = "areaBasedFallback")
    @CircuitBreaker(name = "tourApiAreaBased", fallbackMethod = "areaBasedFallback")
    public List<TourItem> areaBasedList(LDongCode region, Integer contentTypeId, int pageNo, int numOfRows) {
        log.debug("TourAPI areaBasedList2 call: regnCd={}, signguCd={}, contentTypeId={}, page={}, rows={} (serviceKey={})",
                region.lDongRegnCd(), region.lDongSignguCd(), contentTypeId, pageNo, numOfRows,
                LogMaskingUtil.maskServiceKey("serviceKey=" + props.serviceKey()));

        JsonNode root = tourApiRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/areaBasedList2")
                            .queryParam("serviceKey", props.serviceKey())
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "Dallyeo")
                            .queryParam("_type", "json")
                            .queryParam("arrange", "A")
                            .queryParam("numOfRows", numOfRows)
                            .queryParam("pageNo", pageNo)
                            .queryParam("lDongRegnCd", region.lDongRegnCd())
                            .queryParam("lDongSignguCd", region.lDongSignguCd());
                    if (contentTypeId != null) {
                        uriBuilder.queryParam("contentTypeId", contentTypeId);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(JsonNode.class);

        List<JsonNode> rawItems = extractItems(root);
        return normalizer.normalizeItems(rawItems, TourApiNormalizer.Operation.AREA_BASED);
    }

    /** 재시도 소진/서킷 오픈/타임아웃/HTTP 오류 → 502 EXTERNAL_API_ERROR 로 정규화 (BR-7). */
    @SuppressWarnings("unused")
    private List<TourItem> areaBasedFallback(LDongCode region, Integer contentTypeId,
                                             int pageNo, int numOfRows, Throwable t) {
        log.warn("TourAPI areaBasedList2 fallback (regnCd={}, signguCd={}): {}",
                region.lDongRegnCd(), region.lDongSignguCd(), t.toString());
        throw new ExternalApiException("TourAPI 지역기반 목록 조회에 실패했습니다.", t);
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
