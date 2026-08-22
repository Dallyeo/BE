package com.ppip.dallyeo.course;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.course.dto.CourseSummary;
import com.ppip.dallyeo.domain.region.Region;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 컨트롤러 파라미터 파싱/위임 단위 테스트 (필터 400, BR-U2-4). MVC 슬라이스 대신 직접 호출.
 */
class CourseControllerTest {

    private final CourseQueryService service = mock(CourseQueryService.class);
    private final CourseController controller = new CourseController(service);

    @Test
    void list_validFilters_delegates() {
        when(service.findSummaries(eq(Region.GUNSAN), eq(CourseDistance.MEDIUM)))
                .thenReturn(List.of(new CourseSummary("id", "n", "설명", Region.GUNSAN, CourseDistance.MEDIUM, 100, 3)));

        ApiResponse<List<CourseSummary>> res = controller.list("GUNSAN", "MEDIUM");

        assertThat(res.success()).isTrue();
        assertThat(res.data()).hasSize(1);
    }

    @Test
    void list_noFilters_passesNulls() {
        when(service.findSummaries(null, null)).thenReturn(List.of());
        ApiResponse<List<CourseSummary>> res = controller.list(null, null);
        assertThat(res.success()).isTrue();
    }

    @Test
    void list_invalidRegion_400() {
        assertThatThrownBy(() -> controller.list("SEOUL", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void list_invalidDistance_400() {
        assertThatThrownBy(() -> controller.list(null, "XL"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
