package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.badge.PlaceKey;
import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 장소 검색/목록/반경 오케스트레이션 (US-PLACE-1/2/3). TourApiClient → PlaceMapper → 배지 부착.
 * category는 TourAPI에 coarse contentTypeId로 전달 후, 정확한 CategoryType(예: CAFE)로 후처리 필터.
 *
 * <p>배지는 목록 응답에도 상세와 동일 형태로 포함한다. 항목마다 상세를 다시 부르지 않도록
 * 카테고리 필터를 <b>먼저</b> 적용하고, 남은 항목만 DB 조회 1회로 일괄 매칭한다.
 */
@Service
public class PlaceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_ROWS = 30;

    private final TourApiClient tourApiClient;
    private final PlaceMapper placeMapper;
    private final RegionCodeMapper regionCodeMapper;
    private final BadgeService badgeService;

    public PlaceService(TourApiClient tourApiClient, PlaceMapper placeMapper,
                        RegionCodeMapper regionCodeMapper, BadgeService badgeService) {
        this.tourApiClient = tourApiClient;
        this.placeMapper = placeMapper;
        this.regionCodeMapper = regionCodeMapper;
        this.badgeService = badgeService;
    }

    /** 키워드 검색 (US-PLACE-1). region/category 선택. */
    public List<PlaceSummary> search(String keyword, Region region, CategoryType category) {
        LDongCode ldong = region == null ? null : regionCodeMapper.toLDongCode(region);
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.searchKeyword(keyword, ldong, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return attachBadges(filterByCategory(mapped, category));
    }

    /** 지역 장소 목록 (US-PLACE-2). region 필수. */
    public List<PlaceSummary> listByRegion(Region region, CategoryType category) {
        LDongCode ldong = regionCodeMapper.toLDongCode(region);
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.areaBasedList(ldong, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return attachBadges(filterByCategory(mapped, category));
    }

    /** 반경 주변 (US-PLACE-3). mapX=경도(lng), mapY=위도(lat). */
    public List<PlaceSummary> nearby(double lat, double lng, int radius, CategoryType category) {
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.locationBasedList(lng, lat, radius, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return attachBadges(filterByCategory(mapped, category));
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
