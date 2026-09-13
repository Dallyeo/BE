package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.badge.PlaceKey;
import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 장소 검색/목록/반경 오케스트레이션 (US-PLACE-1/2/3). TourApiClient → PlaceMapper → 배지 부착.
 * category는 TourAPI에 coarse contentTypeId로 전달 후, 정확한 CategoryType(예: CAFE)로 후처리 필터.
 *
 * <p>배지는 목록 응답에도 상세와 동일 형태로 포함한다. 항목마다 상세를 다시 부르지 않도록
 * 카테고리 필터를 <b>먼저</b> 적용하고, 남은 항목만 DB 조회 1회로 일괄 매칭한다.
 *
 * <p>영업시간(businessHours/openHours)도 목록에 싣는다(검색 결과 카드 둘째 줄). 다만 이쪽은
 * TourAPI 목록 응답에 없어 항목마다 detailIntro2가 필요하므로, 카테고리 필터 뒤에
 * {@link BusinessHoursEnricher}가 동시 호출 수·시간 예산 상한을 걸고 병렬로 채운다.
 */
@Service
public class PlaceService {

    private static final int DEFAULT_PAGE = 1;

    /**
     * TourAPI 1페이지 조회 건수.
     *
     * <p>지역 단위 조회는 "그 지역 전체"를 보여줘야 하는데(V3 지도), 이전 값 30은 근거 없는 상수라
     * 군산 음식점·카페 129건 중 30건만 노출되고 있었다. 200이면 한 번 호출로 전량이 들어와
     * 페이지 순회가 필요 없다. 응답은 Redis에 30분 캐시되므로 TourAPI 호출량은 늘지 않는다.
     */
    private static final int DEFAULT_ROWS = 200;

    private final TourApiClient tourApiClient;
    private final PlaceMapper placeMapper;
    private final RegionCodeMapper regionCodeMapper;
    private final BadgeService badgeService;
    private final BusinessHoursEnricher businessHoursEnricher;

    public PlaceService(TourApiClient tourApiClient, PlaceMapper placeMapper,
                        RegionCodeMapper regionCodeMapper, BadgeService badgeService,
                        BusinessHoursEnricher businessHoursEnricher) {
        this.tourApiClient = tourApiClient;
        this.placeMapper = placeMapper;
        this.regionCodeMapper = regionCodeMapper;
        this.badgeService = badgeService;
        this.businessHoursEnricher = businessHoursEnricher;
    }

    /** 키워드 검색 (US-PLACE-1). region/category 선택. */
    public List<PlaceSummary> search(String keyword, Region region, CategoryType category) {
        Integer typeId = coarseTypeId(category);
        List<TourItem> items = region == null
                ? tourApiClient.searchKeyword(keyword, null, typeId, DEFAULT_PAGE, DEFAULT_ROWS)
                : mergeByRegion(region, ldong ->
                        tourApiClient.searchKeyword(keyword, ldong, typeId, DEFAULT_PAGE, DEFAULT_ROWS));
        return toResponse(items, category);
    }

    /** 지역 장소 목록 (US-PLACE-2). region 필수. */
    public List<PlaceSummary> listByRegion(Region region, CategoryType category) {
        Integer typeId = coarseTypeId(category);
        return toResponse(mergeByRegion(region, ldong ->
                tourApiClient.areaBasedList(ldong, typeId, DEFAULT_PAGE, DEFAULT_ROWS)), category);
    }

    /**
     * 지역의 법정동코드마다 조회해 하나로 합친다.
     *
     * <p>전주는 시(110)에 TourAPI 데이터가 0건이고 실제 데이터가 완산구(111)·덕진구(113)에
     * 나뉘어 있어, 코드 하나만 부르면 빈 배열이 나온다. 자치구가 하나인 군산은 1회 호출 그대로다.
     *
     * <p>합친 뒤 contentId로 중복을 제거하고 이름순으로 재정렬한다 — 코드별로 이어붙이기만 하면
     * "완산구 전체 → 덕진구 전체" 순이 되어 한 지역인데 목록이 두 덩어리로 보인다.
     */
    private List<TourItem> mergeByRegion(Region region, Function<LDongCode, List<TourItem>> fetch) {
        List<LDongCode> codes = regionCodeMapper.toLDongCodes(region);
        if (codes.size() == 1) {
            return fetch.apply(codes.get(0));
        }
        Map<String, TourItem> byId = new LinkedHashMap<>();
        for (LDongCode ldong : codes) {
            for (TourItem item : fetch.apply(ldong)) {
                byId.putIfAbsent(item.contentId(), item);
            }
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(TourItem::title, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    /** 반경 주변 (US-PLACE-3). mapX=경도(lng), mapY=위도(lat). */
    public List<PlaceSummary> nearby(double lat, double lng, int radius, CategoryType category) {
        return toResponse(tourApiClient.locationBasedList(
                lng, lat, radius, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS), category);
    }

    /**
     * TourAPI 항목 → 응답 목록. 순서가 중요하다: 카테고리 필터를 먼저 걸어 부착 대상 자체를 줄이고,
     * 그 다음 배지(DB 1회)와 영업시간(detailIntro2 병렬)을 채운다.
     */
    private List<PlaceSummary> toResponse(List<TourItem> items, CategoryType category) {
        List<PlaceSummary> filtered = filterByCategory(placeMapper.toSummaries(items), category);
        return businessHoursEnricher.enrich(attachBadges(filtered), contentTypeById(items));
    }

    /** detailIntro2에 필요한 contentId → contentTypeId. 목록 응답에서만 얻을 수 있다. */
    private Map<String, Integer> contentTypeById(List<TourItem> items) {
        Map<String, Integer> byId = new HashMap<>();
        for (TourItem item : items) {
            if (item.contentId() != null) {
                byId.putIfAbsent(item.contentId(), item.contentTypeId());
            }
        }
        return byId;
    }

    /** 목록 전체에 배지 부착 — DB 조회 1회(N+1 방지). 미매칭 항목은 빈 배열 유지. */
    private List<PlaceSummary> attachBadges(List<PlaceSummary> list) {
        if (list.isEmpty()) {
            return list;
        }
        Map<PlaceKey, List<String>> badges = badgeService.badgesForAll(
                list.stream().map(p -> new PlaceKey(p.name(), p.address())).distinct().toList());
        return list.stream()
                .map(p -> p.withBadges(badges.getOrDefault(new PlaceKey(p.name(), p.address()), List.of())))
                .toList();
    }

    /** CategoryType → TourAPI contentTypeId (coarse). CAFE/RESTAURANT는 39 공유 → 후처리로 구분. */
    private Integer coarseTypeId(CategoryType category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case TOUR -> 12;
            case RESTAURANT, CAFE -> 39;
            case CULTURE -> 14;
            case FESTIVAL -> 15;
            case TRAVEL_COURSE -> 25;
            case LEPORTS -> 28;
            case STAY -> 32;
            case SHOPPING -> 38;
            case ETC -> null;
        };
    }

    /** 요청 category와 정확히 일치하는 항목만(예: 39 중 CAFE만). category 미지정이면 전체. */
    private List<PlaceSummary> filterByCategory(List<PlaceSummary> list, CategoryType category) {
        if (category == null) {
            return list;
        }
        return list.stream().filter(p -> p.category() == category).toList();
    }
}
