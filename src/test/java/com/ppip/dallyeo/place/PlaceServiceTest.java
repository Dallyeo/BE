package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.badge.PlaceKey;
import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 목록 응답 부가정보(배지·영업시간) 부착 검증.
 * 배지는 상세와 동일 형태로 싣되 항목마다 조회하지 않고 일괄 조회 1회로 끝내는 것이 요점이고,
 * 영업시간은 목록 응답에 없어 항목별 detailIntro2로 채우되 실패해도 목록은 나와야 하는 것이 요점이다.
 */
class PlaceServiceTest {

    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final RegionCodeMapper regionCodeMapper = new RegionCodeMapper();
    private final BadgeService badgeService = mock(BadgeService.class);
    private final TourApiProperties props = new TourApiProperties(
            "http://localhost", "k", Duration.ofSeconds(2), Duration.ofSeconds(3),
            Duration.ofMinutes(30), Duration.ofHours(24), 4, Duration.ofSeconds(5));
    private final PlaceService service = new PlaceService(
            tourApiClient, new PlaceMapper(new CategoryMapper()), regionCodeMapper, badgeService,
            new BusinessHoursEnricher(tourApiClient, props));

    private TourItem item(String id, String title, String address) {
        return new TourItem(id, title, 35.9, 126.7, null, address, null, 39, null, null, null);
    }

    private final TourItem badged = item("1", "아서원", "전북특별자치도 군산시 경암5길 63");
    private final TourItem plain = item("2", "무명식당", "전북 군산시 어딘가 5");

    private void stubBadges() {
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of(
                new PlaceKey("아서원", "전북특별자치도 군산시 경암5길 63"), List.of("MODEL_RESTAURANT"),
                new PlaceKey("무명식당", "전북 군산시 어딘가 5"), List.of()));
    }

    @Test
    void search_attachesBadges() {
        when(tourApiClient.searchKeyword(eq("아서원"), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(badged, plain));
        stubBadges();

        List<PlaceSummary> res = service.search("아서원", Region.GUNSAN, null);

        assertThat(res).extracting(PlaceSummary::badges)
                .containsExactly(List.of("MODEL_RESTAURANT"), List.of());
    }

    @Test
    void listByRegion_attachesBadges() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(badged, plain));
        stubBadges();

        assertThat(service.listByRegion(Region.GUNSAN, null))
                .extracting(PlaceSummary::badges)
                .containsExactly(List.of("MODEL_RESTAURANT"), List.of());
    }

    @Test
    void nearby_attachesBadges() {
        // 서비스는 (mapX=lng, mapY=lat) 순으로 넘긴다. 페이지/건수는 상수라 매처로 둔다.
        when(tourApiClient.locationBasedList(eq(126.7), eq(35.9), eq(1000), any(), anyInt(), anyInt()))
                .thenReturn(List.of(badged, plain));
        stubBadges();

        assertThat(service.nearby(35.9, 126.7, 1000, null))
                .extracting(PlaceSummary::badges)
                .containsExactly(List.of("MODEL_RESTAURANT"), List.of());
    }

    @Test
    void badgeLookupHappensOncePerRequest() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(badged, plain));
        stubBadges();

        service.listByRegion(Region.GUNSAN, null);

        verify(badgeService).badgesForAll(anyCollection());
    }

    @Test
    void unmatchedPlaceGetsEmptyBadgesNotNull() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(plain));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        assertThat(service.listByRegion(Region.GUNSAN, null).get(0).badges()).isNotNull().isEmpty();
    }

    @Test
    void listCarriesBusinessHoursFromDetailIntro() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(tourApiClient.detailIntroBulk("1", 39)).thenReturn(
                new TourIntro("12:00~21:00\n준비시간 14:00~17:00", null, null, null));

        PlaceSummary res = service.listByRegion(Region.GUNSAN, null).get(0);

        assertThat(res.businessHours()).isEqualTo("12:00~21:00\n준비시간 14:00~17:00");
        assertThat(res.openHours()).isEqualTo("12:00~21:00");   // 카드 한 줄용 대표값
    }

    @Test
    void detailIntroFailureLeavesHoursNullButKeepsList() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(tourApiClient.detailIntroBulk("1", 39)).thenThrow(new RuntimeException("502"));

        PlaceSummary res = service.listByRegion(Region.GUNSAN, null).get(0);

        assertThat(res.name()).isEqualTo("아서원");   // 영업시간 하나 때문에 목록이 죽지 않는다
        assertThat(res.businessHours()).isNull();
        assertThat(res.openHours()).isNull();
    }

    @Test
    void emptyResultSkipsIntroLookup() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        assertThat(service.listByRegion(Region.GUNSAN, null)).isEmpty();
        verify(tourApiClient, never()).detailIntroBulk(any(), anyInt());
    }

    @Test
    void emptyResultSkipsBadgeLookup() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        assertThat(service.listByRegion(Region.GUNSAN, null)).isEmpty();
        verify(badgeService, never()).badgesForAll(anyCollection());
    }
}
