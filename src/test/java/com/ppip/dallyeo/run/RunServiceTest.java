package com.ppip.dallyeo.run;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.Course;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import com.ppip.dallyeo.run.dto.RunDetailResponse;
import com.ppip.dallyeo.run.dto.RunSummaryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RunServiceTest {

    private final RunRepository runRepository = mock(RunRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final com.ppip.dallyeo.achievement.AchievementService achievementService =
            mock(com.ppip.dallyeo.achievement.AchievementService.class);
    private final RunService service = new RunService(runRepository, courseRepository, achievementService);

    private final Instant started = Instant.parse("2026-07-09T07:00:00Z");
    private final Instant finished = Instant.parse("2026-07-09T08:00:00Z");

    private RunCreateRequest request(String courseId, Instant start, Instant end) {
        return new RunCreateRequest(courseId,
                List.of(new PolylinePoint(35.95, 126.68)),
                10480, 3600, 343, start, end);
    }

    @Test
    void save_persistsRunOwnedByUser_completionRateNull() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> {
            Run r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RunDetailResponse res = service.save(7L, request(null, started, finished));

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.completionRate()).isNull();
        assertThat(res.distanceMeters()).isEqualTo(10480);
        verify(runRepository).save(any(Run.class));
    }

    @Test
    void save_looseCourseId_savesEvenWhenCourseMissing() {
        when(courseRepository.findById("ghost")).thenReturn(Optional.empty());
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        RunDetailResponse res = service.save(7L, request("ghost", started, finished));

        assertThat(res.courseId()).isEqualTo("ghost");
        assertThat(res.courseName()).isNull();
    }

    @Test
    void save_finishedBeforeStarted_throwsBadRequest() {
        assertThatThrownBy(() -> service.save(7L, request(null, finished, started)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BAD_REQUEST);

        verify(runRepository, never()).save(any());
    }

    @Test
    void list_returnsOwnRunsWithCourseName() {
        Run run = Run.builder().id(1L).userId(7L).courseId("gunsan-modern-history-run")
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .startedAt(started).finishedAt(finished).build();
        when(runRepository.findByOwnerAndPeriod(eq(7L), any(), any())).thenReturn(List.of(run));
        when(courseRepository.findById("gunsan-modern-history-run"))
                .thenReturn(Optional.of(Course.builder().id("gunsan-modern-history-run").name("근대 역사 박물관 런").build()));

        List<RunSummaryResponse> res = service.list(7L, null, null);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).courseName()).isEqualTo("근대 역사 박물관 런");
    }

    @Test
    void list_invalidDate_throwsBadRequest() {
        assertThatThrownBy(() -> service.list(7L, "not-a-date", null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void getDetail_ownRun_returnsDetail() {
        Run run = Run.builder().id(1L).userId(7L)
                .polyline(List.of(new PolylinePoint(35.95, 126.68)))
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        RunDetailResponse res = service.getDetail(7L, 1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.polyline()).hasSize(1);
    }

    @Test
    void getDetail_othersRun_throwsNotFound() {
        Run run = Run.builder().id(1L).userId(99L).startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getDetail(7L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void getDetail_missing_throwsNotFound() {
        when(runRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(7L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }
}
