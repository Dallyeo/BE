package com.ppip.dallyeo.place;

import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 장소 검색/목록/반경 오케스트레이션 (US-PLACE-1/2/3). TourApiClient → PlaceMapper.
 * category는 TourAPI에 coarse contentTypeId로 전달 후, 정확한 CategoryType(예: CAFE)로 후처리 필터.
 */
@Service
public class PlaceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_ROWS = 30;

    private final TourApiClient tourApiClient;
    private final PlaceMapper placeMapper;
    private final RegionCodeMapper regionCodeMapper;

    public PlaceService(TourApiClient tourApiClient, PlaceMapper placeMapper, RegionCodeMapper regionCodeMapper) {
        this.tourApiClient = tourApiClient;
        this.placeMapper = placeMapper;
        this.regionCodeMapper = regionCodeMapper;
    }

    /** 키워드 검색 (US-PLACE-1). region/category 선택. */
    public List<PlaceSummary> search(String keyword, Region region, CategoryType category) {
        LDongCode ldong = region == null ? null : regionCodeMapper.toLDongCode(region);
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.searchKeyword(keyword, ldong, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return filterByCategory(mapped, category);
    }

    /** 지역 장소 목록 (US-PLACE-2). region 필수. */
    public List<PlaceSummary> listByRegion(Region region, CategoryType category) {
        LDongCode ldong = regionCodeMapper.toLDongCode(region);
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.areaBasedList(ldong, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return filterByCategory(mapped, category);
    }

    /** 반경 주변 (US-PLACE-3). mapX=경도(lng), mapY=위도(lat). */
    public List<PlaceSummary> nearby(double lat, double lng, int radius, CategoryType category) {
        List<PlaceSummary> mapped = placeMapper.toSummaries(
                tourApiClient.locationBasedList(lng, lat, radius, coarseTypeId(category), DEFAULT_PAGE, DEFAULT_ROWS));
        return filterByCategory(mapped, category);
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
