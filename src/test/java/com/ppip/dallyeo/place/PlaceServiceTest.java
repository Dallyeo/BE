package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.badge.PlaceKey;
import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCodeMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
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
    private final BusinessHoursStore hoursStore = mock(BusinessHoursStore.class);
    private final TourApiProperties props = new TourApiProperties(
            "http://localhost", "k", Duration.ofSeconds(2), Duration.ofSeconds(3),
            Duration.ofMinutes(30), Duration.ofHours(24), Duration.ofDays(30), 4, Duration.ofSeconds(5));
    private final PlaceService service = new PlaceService(
            tourApiClient, new PlaceMapper(new CategoryMapper()), regionCodeMapper, badgeService,
            new BusinessHoursEnricher(tourApiClient, hoursStore, props));

    private TourItem item(String id, String title, String address) {
        return new TourItem(id, title, 35.9, 126.7, null, address, null, 39, null, null, null);
    }

    private final TourItem badged = item("1", "아서원", "전북특별자치도 군산시 경암5길 63");
    private final TourItem plain = item("2", "무명식당", "전북 군산시 어딘가 5");

    @org.junit.jupiter.api.BeforeEach
    void emptyStore() {
        // 보관소가 비어 있는 상태 = 전부 TourAPI로 받아오는 경로
        when(hoursStore.findFresh(anyCollection())).thenReturn(Map.of());
    }

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
    // ===== 전주: 자치구 2개 병합 (기존 결함 — 110만 쓰면 0건) =====

    private TourItem item(String id, String title, String address, int typeId) {
        return new TourItem(id, title, 35.8, 127.1, null, address, null, typeId, null, null, null);
    }

    @Test
    void jeonju_queriesBothDistrictsAndMerges() {
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "111")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("w1", "완산집", "전주시 완산구 1", 39)));
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "113")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("d1", "덕진집", "전주시 덕진구 1", 39)));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        List<PlaceSummary> res = service.listByRegion(Region.JEONJU, null);

        assertThat(res).extracting(PlaceSummary::id).containsExactlyInAnyOrder("w1", "d1");
        // 110(전주시)은 데이터가 0건이라 부르면 안 된다
        verify(tourApiClient, never()).areaBasedList(eq(new LDongCode("52", "110")), any(), anyInt(), anyInt());
    }

    @Test
    void jeonju_mergedListIsSortedByNameNotByDistrict() {
        // 구별로 이어붙이기만 하면 "완산구 전체 → 덕진구 전체" 로 두 덩어리가 된다.
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "111")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("w1", "가게", "완산 1", 39), item("w2", "하게", "완산 2", 39)));
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "113")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("d1", "나게", "덕진 1", 39)));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        assertThat(service.listByRegion(Region.JEONJU, null))
                .extracting(PlaceSummary::name).containsExactly("가게", "나게", "하게");
    }

    @Test
    void jeonju_deduplicatesSameContentIdAcrossDistricts() {
        TourItem dup = item("same", "겹친집", "전주 어딘가", 39);
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "111")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dup));
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "113")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dup));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        assertThat(service.listByRegion(Region.JEONJU, null)).hasSize(1);
    }

    @Test
    void gunsan_stillOneCallNoMergeOverhead() {
        when(tourApiClient.areaBasedList(eq(new LDongCode("52", "130")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(badged));
        stubBadges();

        assertThat(service.listByRegion(Region.GUNSAN, null)).hasSize(1);
        verify(tourApiClient).areaBasedList(any(), any(), anyInt(), anyInt());   // 정확히 1회
    }

    @Test
    void search_withJeonjuRegion_alsoMergesDistricts() {
        when(tourApiClient.searchKeyword(eq("비빔밥"), eq(new LDongCode("52", "111")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("w1", "완산비빔밥", "완산 1", 39)));
        when(tourApiClient.searchKeyword(eq("비빔밥"), eq(new LDongCode("52", "113")), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("d1", "덕진비빔밥", "덕진 1", 39)));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        assertThat(service.search("비빔밥", Region.JEONJU, null))
                .extracting(PlaceSummary::id).containsExactlyInAnyOrder("w1", "d1");
    }

    @Test
    void search_withoutRegion_doesNotSplit() {
        when(tourApiClient.searchKeyword(eq("커피"), eq(null), any(), anyInt(), anyInt()))
                .thenReturn(List.of(item("x", "커피집", "어딘가", 39)));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());

        assertThat(service.search("커피", null, null)).hasSize(1);
    }
    // ===== 영업시간 보관소 (일일 호출 한도 절감의 핵심) =====

    @Test
    void storedHours_skipTourApiEntirely() {
        // 보관분이 있으면 외부 호출이 아예 없어야 한다 — 매일 다시 받는 게 한도를 태우던 원인.
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(hoursStore.findFresh(anyCollection()))
                .thenReturn(Map.of("1", Optional.of("11:00~21:00")));

        PlaceSummary res = service.listByRegion(Region.GUNSAN, null).get(0);

        assertThat(res.businessHours()).isEqualTo("11:00~21:00");
        assertThat(res.openHours()).isEqualTo("11:00~21:00");
        verify(tourApiClient, never()).detailIntroBulk(any(), anyInt());
    }

    @Test
    void storedAsNoHours_isRemembered_notRefetched() {
        // "원천에 영업시간 없음"도 보관 대상 — 아니면 그런 장소를 매번 다시 묻게 된다.
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(hoursStore.findFresh(anyCollection())).thenReturn(Map.of("1", Optional.empty()));

        assertThat(service.listByRegion(Region.GUNSAN, null).get(0).businessHours()).isNull();
        verify(tourApiClient, never()).detailIntroBulk(any(), anyInt());
    }

    @Test
    void freshlyFetchedHours_arePersistedForNextTime() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(tourApiClient.detailIntroBulk("1", 39))
                .thenReturn(new TourIntro("11:00~21:00", null, null, null));

        service.listByRegion(Region.GUNSAN, null);

        ArgumentCaptor<List<PlaceBusinessHours>> saved = ArgumentCaptor.forClass(List.class);
        verify(hoursStore).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement().satisfies(row -> {
            assertThat(row.getContentId()).isEqualTo("1");
            assertThat(row.getBusinessHours()).isEqualTo("11:00~21:00");
        });
    }

    @Test
    void placeWithoutHoursAtSource_isPersistedAsNull() {
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(tourApiClient.detailIntroBulk("1", 39))
                .thenReturn(new TourIntro(null, null, null, null));   // 조회 성공, 정보 없음

        service.listByRegion(Region.GUNSAN, null);

        ArgumentCaptor<List<PlaceBusinessHours>> saved = ArgumentCaptor.forClass(List.class);
        verify(hoursStore).saveAll(saved.capture());
        assertThat(saved.getValue()).singleElement()
                .satisfies(row -> assertThat(row.getBusinessHours()).isNull());
    }

    @Test
    void failedLookup_isNotPersisted_soItRetriesLater() {
        // 실패는 "정보 없음"과 다르다 — 보관하면 영영 다시 안 받는다.
        when(tourApiClient.areaBasedList(any(), any(), anyInt(), anyInt())).thenReturn(List.of(badged));
        when(badgeService.badgesForAll(anyCollection())).thenReturn(Map.of());
        when(tourApiClient.detailIntroBulk("1", 39)).thenThrow(new RuntimeException("한도 초과"));

        service.listByRegion(Region.GUNSAN, null);

        ArgumentCaptor<List<PlaceBusinessHours>> saved = ArgumentCaptor.forClass(List.class);
        verify(hoursStore).saveAll(saved.capture());
        assertThat(saved.getValue()).isEmpty();
    }
}
