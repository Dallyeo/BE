package com.ppip.dallyeo.run;

import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import com.ppip.dallyeo.run.dto.RunDetailResponse;
import com.ppip.dallyeo.run.dto.RunSummaryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunControllerTest {

    private final RunService runService = mock(RunService.class);
    private final RunController controller = new RunController(runService);

    private final Instant started = Instant.parse("2026-07-09T07:00:00Z");
    private final Instant finished = Instant.parse("2026-07-09T08:00:00Z");

    private RunDetailResponse detail() {
        return new RunDetailResponse(1L, null, null,
                List.of(new PolylinePoint(35.95, 126.68)),
                10480, 3600, 343, null, started, finished);
    }

    @Test
    void save_delegatesAndWraps() {
        RunCreateRequest req = new RunCreateRequest(null,
                List.of(new PolylinePoint(35.95, 126.68)), 10480, 3600, 343, started, finished);
        when(runService.save(7L, req)).thenReturn(detail());

        ApiResponse<RunDetailResponse> res = controller.save(7L, req);

        assertThat(res.success()).isTrue();
        assertThat(res.data().id()).isEqualTo(1L);
    }

    @Test
    void list_delegatesWithPeriod() {
        RunSummaryResponse summary = new RunSummaryResponse(1L, "근대 역사 박물관 런", 10480, 3600, finished);
        when(runService.list(7L, "2026-07-01", "2026-07-31")).thenReturn(List.of(summary));

        ApiResponse<List<RunSummaryResponse>> res = controller.list(7L, "2026-07-01", "2026-07-31");

        assertThat(res.data()).hasSize(1);
        assertThat(res.data().get(0).courseName()).isEqualTo("근대 역사 박물관 런");
    }

    @Test
    void detail_delegates() {
        when(runService.getDetail(7L, 1L)).thenReturn(detail());

        ApiResponse<RunDetailResponse> res = controller.detail(7L, 1L);

        assertThat(res.data().id()).isEqualTo(1L);
        verify(runService).getDetail(7L, 1L);
    }
}
