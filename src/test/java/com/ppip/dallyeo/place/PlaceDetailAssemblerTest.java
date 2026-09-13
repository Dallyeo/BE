package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.domain.category.CategoryMapper;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.place.dto.PlaceDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceDetailAssemblerTest {

    private final TourApiClient client = mock(TourApiClient.class);
    private final BadgeService badgeService = mock(BadgeService.class);
    private final PlaceMapper mapper = new PlaceMapper(new CategoryMapper());
    private final PlaceDetailAssembler assembler = new PlaceDetailAssembler(client, mapper, badgeService);

    @Test
    void assemble_composesCommonThenIntroWithType() {
        TourCommon common = new TourCommon("100", "군산맛집", 39, "개요", null, "http://img",
                null, "전북 군산시", 35.9, 126.7);
        when(client.detailCommon("100")).thenReturn(common);
        when(client.detailIntro("100", 39)).thenReturn(new TourIntro("11:30~19:00", null, null, null));
        when(badgeService.badgesFor("군산맛집", "전북 군산시")).thenReturn(List.of("MODEL_RESTAURANT"));

        PlaceDetail d = assembler.assemble("100");

        assertThat(d.businessHours()).isEqualTo("11:30~19:00");
        assertThat(d.badges()).containsExactly("MODEL_RESTAURANT");
        verify(client).detailIntro("100", 39);   // P3: common의 typeId로 순차 호출
    }

    @Test
    void assemble_survivesWhenHoursLookupFails() {
        // detailIntro2 오퍼레이션만 한도를 소진해도 개요·좌표·배지는 멀쩡하다 —
        // 영업시간 하나 때문에 상세 화면 전체가 502로 죽으면 안 된다.
        TourCommon common = new TourCommon("100", "군산맛집", 39, "개요", null, "http://img",
                null, "전북 군산시", 35.9, 126.7);
        when(client.detailCommon("100")).thenReturn(common);
        when(client.detailIntro("100", 39)).thenReturn(null);   // 조회 실패 → fallback이 null 반환
        when(badgeService.badgesFor("군산맛집", "전북 군산시")).thenReturn(List.of("GOOD_PRICE"));

        PlaceDetail d = assembler.assemble("100");

        assertThat(d.name()).isEqualTo("군산맛집");
        assertThat(d.businessHours()).isNull();
        assertThat(d.openHours()).isNull();
        assertThat(d.badges()).containsExactly("GOOD_PRICE");
    }

    @Test
    void assemble_notFoundWhenCommonNull() {
        when(client.detailCommon("nope")).thenReturn(null);

        assertThatThrownBy(() -> assembler.assemble("nope"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
