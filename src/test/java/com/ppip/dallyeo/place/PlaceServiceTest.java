package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.badge.PlaceKey;
import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.junit.jupiter.api.Test;

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
 * 목록 응답 배지 부착 검증. 상세({@code /places/{id}})와 동일 형태의 badges를 목록에도 싣되,
 * 항목마다 조회하지 않고 일괄 조회 1회로 끝내는 것이 요점.
 */
class PlaceServiceTest {

    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final RegionCodeMapper regionCodeMapper = new RegionCodeMapper();
    private final BadgeService badgeService = mock(BadgeService.class);
    private final PlaceService service = new PlaceService(
            tourApiClient, new PlaceMapper(new CategoryMapper()), regionCodeMapper, badgeService);

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
        // 서비스는 (mapX=lng, mapY=lat) 순으로 넘긴다.
        when(tourApiClient.locationBasedList(126.7, 35.9, 1000, null, 1, 30))
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
    void emptyResultSkipsBadgeLookup() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        assertThat(service.listByRegion(Region.GUNSAN, null)).isEmpty();
        verify(badgeService, never()).badgesForAll(anyCollection());
    }
}
