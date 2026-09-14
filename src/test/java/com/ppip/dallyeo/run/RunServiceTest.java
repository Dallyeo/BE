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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.mockito.ArgumentCaptor;
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

    private final PolylinePoint START = new PolylinePoint(35.95, 126.68);
    private final PolylinePoint END = new PolylinePoint(35.96, 126.69);
    private final MultipartFile image = new MockMultipartFile(
            "image", "route.jpg", "image/jpeg", "fake".getBytes());

    private RunCreateRequest request(String courseId, Instant start, Instant end) {
        return new RunCreateRequest(null, courseId, START, END, 10480, 3600, null, start, end);
    }

    private RunDetailResponse save(Long userId, RunCreateRequest request) {
        return service.save(userId, request, image);
    }

    @Test
    void save_persistsRunOwnedByUser_completionRateNull() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> {
            Run r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        RunDetailResponse res = save(7L, request(null, started, finished));

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.completionRate()).isNull();
        assertThat(res.distanceMeters()).isEqualTo(10480);
        verify(runRepository).save(any(Run.class));
    }

    @Test
    void save_looseCourseId_savesEvenWhenCourseMissing() {
        when(courseRepository.findById("ghost")).thenReturn(Optional.empty());
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        RunDetailResponse res = save(7L, request("ghost", started, finished));

        assertThat(res.courseId()).isEqualTo("ghost");
        assertThat(res.courseName()).isNull();
    }

    @Test
    void save_finishedBeforeStarted_throwsBadRequest() {
        assertThatThrownBy(() -> save(7L, request(null, finished, started)))
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
                .startLat(35.95).startLng(126.68).endLat(35.96).endLng(126.69)
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .imageUrl("/uploads/runs/x.jpg")
                .startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        RunDetailResponse res = service.getDetail(7L, 1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.start()).isEqualTo(new PolylinePoint(35.95, 126.68));
        assertThat(res.end()).isEqualTo(new PolylinePoint(35.96, 126.69));
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

    private Run ownedRun() {
        return Run.builder().id(1L).userId(7L)
                .startLat(35.95).startLng(126.68).endLat(35.96).endLng(126.69)
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .imageUrl("/uploads/runs/old.jpg")
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

        RunDetailResponse res = save(7L, request("gunsan-jjamppong-run", started, finished));

        assertThat(res.newAchievements()).extracting(AchievementResponse::code).containsExactly("JJAMPPONG");
    }

    @Test
    void save_noNewAchievement_returnsEmptyNotNull() {
        // 이미 달성한 조건을 다시 채운 경우 — 도장이 뜨면 안 된다(빈 배열).
        stubSavedRun();
        when(achievementService.evaluateAndUnlock(7L)).thenReturn(List.of());

        RunDetailResponse res = save(7L, request("gunsan-jjamppong-run", started, finished));

        assertThat(res.newAchievements()).isNotNull().isEmpty();
    }

    @Test
    void getDetail_neverCarriesAchievements() {
        // 지난 기록을 다시 열어도 도장이 재생되면 안 된다 → 조회 응답엔 아예 없음(null → 직렬화 제외).
        Run run = Run.builder().id(1L).userId(7L).courseId("gunsan-jjamppong-run")
                .startLat(35.95).startLng(126.68).endLat(35.96).endLng(126.69)
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(343)
                .imageUrl("/uploads/runs/x.jpg")
                .startedAt(started).finishedAt(finished).build();
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        assertThat(service.getDetail(7L, 1L).newAchievements()).isNull();
    }
    // ===== 새 저장 구조 (좌표 2점 · 이미지 필수 · 날짜 기본값 · 페이스 계산) =====

    @Test
    void save_storesStartAndEndCoordinates() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageStorage.store(any(), any())).thenReturn("/uploads/runs/a.jpg");

        RunDetailResponse res = save(7L, request(null, started, finished));

        assertThat(res.start()).isEqualTo(START);
        assertThat(res.end()).isEqualTo(END);
        assertThat(res.imageUrl()).isEqualTo("/uploads/runs/a.jpg");
    }

    @Test
    void save_withoutFinishedAt_usesServerTimeAsRunDate() {
        // 날짜는 클라이언트가 주면 그 값, 없으면 저장 시각.
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));
        Instant before = Instant.now();

        RunDetailResponse res = save(7L, new RunCreateRequest(null, null, START, END, 10480, 3600, null, null, null));

        assertThat(res.finishedAt()).isBetween(before, Instant.now());
        assertThat(res.startedAt()).isNull();   // 선택 필드 — 없어도 저장된다
    }

    @Test
    void save_withFinishedAt_keepsClientDate() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(save(7L, request(null, started, finished)).finishedAt()).isEqualTo(finished);
    }

    @Test
    void save_computesPaceFromDistanceAndDuration() {
        // 페이스는 클라이언트가 주지 않는다 — 거리·시간에서 서버가 계산한다.
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        // 10.48km 를 3600초 → 3600*1000/10480 = 343.5 → 반올림 344초/km
        assertThat(save(7L, request(null, started, finished)).averagePaceSeconds()).isEqualTo(344);
    }

    @Test
    void save_imageIsStoredOncePerRun() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        save(7L, request(null, started, finished));

        verify(imageStorage).store(eq(image), eq("runs"));
    }
    // ===== 멱등키(중복 저장 방지) =====

    @Test
    void sameClientRunId_returnsExistingRunInsteadOfCreatingAnother() {
        // 저장은 됐는데 응답이 유실돼 재전송하는 상황.
        Run existing = Run.builder().id(42L).userId(7L).clientRunId("abc")
                .startLat(35.95).startLng(126.68).endLat(35.96).endLng(126.69)
                .distanceMeters(10480).durationSeconds(3600).averagePaceSeconds(344)
                .imageUrl("/uploads/runs/first.jpg").startedAt(started).finishedAt(finished).build();
        when(runRepository.findByUserIdAndClientRunId(7L, "abc")).thenReturn(Optional.of(existing));

        RunDetailResponse res = service.save(7L,
                new RunCreateRequest("abc", null, START, END, 10480, 3600, null, started, finished), image);

        assertThat(res.id()).isEqualTo(42L);
        assertThat(res.imageUrl()).isEqualTo("/uploads/runs/first.jpg");
        verify(runRepository, never()).save(any());
        // 이미지도 다시 저장하지 않는다 — 고아 파일이 생기면 안 된다.
        verify(imageStorage, never()).store(any(), any());
        // 도장은 첫 요청에서 이미 처리됐다 — 재전송이 다시 띄우면 "최초 1회" 규칙이 깨진다.
        assertThat(res.newAchievements()).isEmpty();
    }

    @Test
    void differentClientRunId_savesSeparately() {
        when(runRepository.findByUserIdAndClientRunId(7L, "new-key")).thenReturn(Optional.empty());
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(7L, new RunCreateRequest("new-key", null, START, END, 10480, 3600, null, started, finished), image);

        verify(runRepository).save(any(Run.class));
    }

    @Test
    void withoutClientRunId_doesNotDeduplicate() {
        // 키를 안 보내면 중복 판단이 불가능하다 — 그대로 새로 저장된다(문서에 명시).
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        save(7L, request(null, started, finished));

        verify(runRepository, never()).findByUserIdAndClientRunId(any(), any());
        verify(runRepository).save(any(Run.class));
    }

    @Test
    void savedRunKeepsClientRunId() {
        when(runRepository.findByUserIdAndClientRunId(7L, "k1")).thenReturn(Optional.empty());
        ArgumentCaptor<Run> saved = ArgumentCaptor.forClass(Run.class);
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        service.save(7L, new RunCreateRequest("k1", null, START, END, 10480, 3600, null, started, finished), image);

        verify(runRepository).save(saved.capture());
        assertThat(saved.getValue().getClientRunId()).isEqualTo("k1");
    }
    @Test
    void save_keepsClientProvidedCalories() {
        // 칼로리는 서버가 추정하지 않고 클라이언트(HealthKit) 값을 그대로 보관한다.
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        RunDetailResponse res = service.save(7L,
                new RunCreateRequest(null, null, START, END, 10480, 3600, 720, started, finished), image);

        assertThat(res.calories()).isEqualTo(720);
    }

    @Test
    void save_withoutCalories_leavesItNull() {
        when(runRepository.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(save(7L, request(null, started, finished)).calories()).isNull();
    }
}
