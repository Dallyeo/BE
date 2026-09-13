package com.ppip.dallyeo.place;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceControllerTest {

    private final PlaceService service = mock(PlaceService.class);
    private final PlaceDetailAssembler assembler = mock(PlaceDetailAssembler.class);
    private final PlaceController controller = new PlaceController(service, assembler);

    @Test
    void search_blankKeyword_400() {
        assertThatThrownBy(() -> controller.search("  ", null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void search_invalidRegion_400() {
        assertThatThrownBy(() -> controller.search("커피", "SEOUL", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void search_validDelegates() {
        when(service.search(eq("커피"), eq(Region.GUNSAN), any()))
                .thenReturn(List.of(new PlaceSummary("1", "카페", null, 35.9, 126.7, "주소",
                        "10:00~22:00", "10:00~22:00", null, null, java.util.List.of())));

        ApiResponse<List<PlaceSummary>> res = controller.search("커피", "GUNSAN", "CAFE");

        assertThat(res.success()).isTrue();
        assertThat(res.data()).hasSize(1);
    }

    @Test
    void nearby_nonPositiveRadius_400() {
        assertThatThrownBy(() -> controller.nearby(35.9, 126.7, 0, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void list_missingRegion_400() {
        assertThatThrownBy(() -> controller.list("  ", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
