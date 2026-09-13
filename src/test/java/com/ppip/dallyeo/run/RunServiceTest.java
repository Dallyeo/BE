package com.ppip.dallyeo.run;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
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
    private final com.ppip.dallyeo.common.storage.ImageStorage imageStorage =
            mock(com.ppip.dallyeo.common.storage.ImageStorage.class);
    private final RunService service =
            new RunService(runRepository, courseRepository, achievementService, imageStorage);

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

    // ===== 기록 이미지 업로드 =====

    private final org.springframework.mock.web.MockMultipartFile image =
            new org.springframework.mock.web.MockMultipartFile("image", "p.jpg", "image/jpeg", "x".getBytes());

    private Run ownedRun() {
        return Run.builder().id(1L).userId(7L)
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .startedAt(started).finishedAt(finished).build();
    }

    @Test
    void attachImage_storesFileAndPersistsUrl() {
        Run run = ownedRun();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorage.store(any(), eq("runs"))).thenReturn("/uploads/runs/abc.jpg");

        RunDetailResponse res = service.attachImage(7L, 1L, image);

        assertThat(res.imageUrl()).isEqualTo("/uploads/runs/abc.jpg");
        assertThat(run.getImageUrl()).isEqualTo("/uploads/runs/abc.jpg");
    }

    @Test
    void attachImage_replacesAndDeletesPreviousFile() {
        Run run = ownedRun();
        run.setImageUrl("/uploads/runs/old.jpg");
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorage.store(any(), eq("runs"))).thenReturn("/uploads/runs/new.jpg");

        service.attachImage(7L, 1L, image);

        verify(imageStorage).deleteQuietly("/uploads/runs/old.jpg");
    }

    @Test
    void attachImage_othersRun_throwsNotFoundAndStoresNothing() {
        Run run = Run.builder().id(1L).userId(99L).startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        assertThatThrownBy(() -> service.attachImage(7L, 1L, image))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);

        verify(imageStorage, never()).store(any(), any());
        verify(runRepository, never()).save(any());
    }

    @Test
    void attachImage_missingRun_throwsNotFound() {
        when(runRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attachImage(7L, 2L, image))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void listAndDetail_exposeImageUrl() {
        Run run = ownedRun();
        run.setImageUrl("/uploads/runs/abc.jpg");
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));
        when(runRepository.findByOwnerAndPeriod(eq(7L), any(), any())).thenReturn(List.of(run));

        assertThat(service.getDetail(7L, 1L).imageUrl()).isEqualTo("/uploads/runs/abc.jpg");
        assertThat(service.list(7L, null, null).get(0).imageUrl()).isEqualTo("/uploads/runs/abc.jpg");
    }
    // ===== 결과창 도장 (POST /runs 응답에만, 최초 달성만) =====

    private void stubSavedRun() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> {
            Run r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
    }

    @Test
    void save_carriesNewlyUnlockedAchievementsForResultScreen() {
        stubSavedRun();
        when(achievementService.evaluateAndUnlock(7L)).thenReturn(List.of(
                new AchievementResponse("JJAMPPONG", "GUNSAN", 30, "짬뽕을 먹을 자격이 있는 자", "설명",
                        "/images/achievements/jjamppong_on.webp", "/images/achievements/jjamppong_off.webp",
                        true, started)));

        RunDetailResponse res = service.save(7L, request("gunsan-jjamppong-run", started, finished));

        assertThat(res.newAchievements()).extracting(AchievementResponse::code).containsExactly("JJAMPPONG");
    }

    @Test
    void save_noNewAchievement_returnsEmptyNotNull() {
        // 이미 달성한 조건을 다시 채운 경우 — 도장이 뜨면 안 된다(빈 배열).
        stubSavedRun();
        when(achievementService.evaluateAndUnlock(7L)).thenReturn(List.of());

        RunDetailResponse res = service.save(7L, request("gunsan-jjamppong-run", started, finished));

        assertThat(res.newAchievements()).isNotNull().isEmpty();
    }

    @Test
    void getDetail_neverCarriesAchievements() {
        // 지난 기록을 다시 열어도 도장이 재생되면 안 된다 → 조회 응답엔 아예 없음(null → 직렬화 제외).
        Run run = Run.builder().id(1L).userId(7L).courseId("gunsan-jjamppong-run")
                .polyline(List.of(new PolylinePoint(35.95, 126.68)))
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        assertThat(service.getDetail(7L, 1L).newAchievements()).isNull();
    }
}
