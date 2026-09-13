package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.Course;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.run.Run;
import com.ppip.dallyeo.run.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementServiceTest {

    private final UserAchievementRepository userAchievementRepository = mock(UserAchievementRepository.class);
    private final RunRepository runRepository = mock(RunRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final AchievementService service =
            new AchievementService(userAchievementRepository, runRepository, courseRepository);

    private static final List<String> GUNSAN_IDS = List.of(
            "gunsan-modern-history-run", "gunsan-saemangeum-run", "gunsan-jjamppong-run",
            "gunsan-seonyudo-beach-run", "gunsan-wetland-eco-run", "gunsan-cypress-forest-run");
    private static final List<String> JEONJU_IDS = List.of(
            "jeonju-hanok-village-run", "jeonju-ajung-lake-run", "jeonju-zoo-run", "jeonju-catholic-shrine-run");

    private List<Course> courses(List<String> ids, Region region) {
        return ids.stream().map(id -> Course.builder().id(id).region(region).build()).toList();
    }

    private List<String> codes(List<AchievementResponse> list) {
        return list.stream().map(AchievementResponse::code).toList();
    }

    /** 기본값은 어떤 임계값 업적에도 걸리지 않는 평범한 러닝(한국시간 낮 1시간 5km). */
    private Run run(String courseId) {
        return run(courseId, "2026-07-09T04:00:00Z", 3600, 5000);   // KST 13:00 시작
    }

    private Run run(String courseId, String startUtc, int durationSeconds, int distanceMeters) {
        Instant start = Instant.parse(startUtc);
        return Run.builder().userId(1L).courseId(courseId)
                .distanceMeters(distanceMeters).durationSeconds(durationSeconds)
                .averagePaceSeconds(durationSeconds / Math.max(1, distanceMeters / 1000))
                .startedAt(start).finishedAt(start.plusSeconds(durationSeconds))
                .build();
    }

    private void stubRuns(Run... runs) {
        when(runRepository.findByOwnerAndPeriod(1L, null, null)).thenReturn(List.of(runs));
    }

    private List<Run> runsFor(List<String> courseIds) {
        return courseIds.stream().map(this::run).toList();
    }

    @BeforeEach
    void stubSave() {
        // save()는 @PrePersist로 unlockedAt이 채워진 엔티티를 돌려준다 — 응답의 달성일시가 여기서 나온다.
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(inv -> {
            UserAchievement ua = inv.getArgument(0);
            ua.setUnlockedAt(Instant.parse("2026-09-09T00:00:00Z"));
            return ua;
        });
    }

    @BeforeEach
    void stubRegions() {
        when(courseRepository.findByFilters(Region.GUNSAN, null)).thenReturn(courses(GUNSAN_IDS, Region.GUNSAN));
        when(courseRepository.findByFilters(Region.JEONJU, null)).thenReturn(courses(JEONJU_IDS, Region.JEONJU));
    }

    @Test
    void evaluate_courseAndRegionAny_unlockedButNotConqueror() {
        stubRuns(run("gunsan-jjamppong-run"));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<String> newly = codes(service.evaluateAndUnlock(1L));

        assertThat(newly).contains("JJAMPPONG", "GUNSAN_BEGINNER");
        assertThat(newly).doesNotContain("GUNSAN_CONQUEROR", "JEONJU_BEGINNER");
        verify(userAchievementRepository, never()).save(argThat(ua -> ua.getAchievement() == AchievementType.GUNSAN_CONQUEROR));
    }

    @Test
    void evaluate_allGunsanCourses_unlocksConqueror() {
        when(runRepository.findByOwnerAndPeriod(1L, null, null)).thenReturn(runsFor(GUNSAN_IDS));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<String> newly = codes(service.evaluateAndUnlock(1L));

        assertThat(newly).contains("GUNSAN_CONQUEROR", "GUNSAN_BEGINNER",
                "JJAMPPONG", "NATURE_LOVER", "BETWEEN_WAVES", "GUNSAN_SEONYUDO");
    }

    @Test
    void evaluate_skipsAlreadyUnlocked() {
        stubRuns(run("gunsan-jjamppong-run"));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(
                UserAchievement.builder().userId(1L).achievement(AchievementType.JJAMPPONG).build()));

        List<String> newly = codes(service.evaluateAndUnlock(1L));

        assertThat(newly).doesNotContain("JJAMPPONG");
        assertThat(newly).contains("GUNSAN_BEGINNER");
    }

    @Test
    void unlock_invalidCode_throwsNotFound() {
        assertThatThrownBy(() -> service.unlock(1L, "NOPE"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void unlock_conditionNotMet_throwsConflict() {
        when(userAchievementRepository.existsByUserIdAndAchievement(1L, AchievementType.JJAMPPONG)).thenReturn(false);
        stubRuns(run("gunsan-wetland-eco-run"));

        assertThatThrownBy(() -> service.unlock(1L, "JJAMPPONG"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void unlock_conditionMet_saves() {
        when(userAchievementRepository.existsByUserIdAndAchievement(1L, AchievementType.JJAMPPONG)).thenReturn(false);
        stubRuns(run("gunsan-jjamppong-run"));
        when(userAchievementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AchievementResponse res = service.unlock(1L, "JJAMPPONG");

        assertThat(res.code()).isEqualTo("JJAMPPONG");
        assertThat(res.unlocked()).isTrue();
        verify(userAchievementRepository).save(any());
    }

    @Test
    void unlock_alreadyUnlocked_idempotentNoSave() {
        when(userAchievementRepository.existsByUserIdAndAchievement(1L, AchievementType.JJAMPPONG)).thenReturn(true);
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(
                UserAchievement.builder().userId(1L).achievement(AchievementType.JJAMPPONG).build()));

        AchievementResponse res = service.unlock(1L, "JJAMPPONG");

        assertThat(res.unlocked()).isTrue();
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void list_returnsWholeCatalogInDesignOrder() {
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(
                UserAchievement.builder().userId(1L).achievement(AchievementType.JJAMPPONG).build()));

        List<AchievementResponse> res = service.list(1L);

        assertThat(res).hasSize(21);
        assertThat(res).anyMatch(a -> a.code().equals("JJAMPPONG") && a.unlocked());
        assertThat(res).anyMatch(a -> a.code().equals("GUNSAN_CONQUEROR") && !a.unlocked());
        // 시안 순서(군산 → 전주 → 공통)대로 정렬되고, 도장 이미지 경로가 코드에서 파생된다.
        assertThat(res).extracting(AchievementResponse::sortOrder).isSorted();
        assertThat(res.get(0).code()).isEqualTo("GUNSAN_SEONYUDO");
        assertThat(res).extracting(AchievementResponse::category)
                .containsSubsequence("GUNSAN", "JEONJU", "COMMON");
        assertThat(res.stream().filter(a -> a.code().equals("JJAMPPONG")).findFirst().orElseThrow())
                .satisfies(a -> {
                    assertThat(a.iconOnUrl()).isEqualTo("/images/achievements/jjamppong_on.webp");
                    assertThat(a.iconOffUrl()).isEqualTo("/images/achievements/jjamppong_off.webp");
                });
    }

    // ===== 신규 판정 종류 =====

    private List<String> evaluateWith(Run... runs) {
        stubRuns(runs);
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());
        return codes(service.evaluateAndUnlock(1L));
    }

    @Test
    void longRun_needsMoreThanThreeHours() {
        assertThat(evaluateWith(run("gunsan-jjamppong-run", "2026-07-09T04:00:00Z", 10_799, 5000)))
                .doesNotContain("LONG_RUN_3H");
        assertThat(evaluateWith(run("gunsan-jjamppong-run", "2026-07-09T04:00:00Z", 10_801, 5000)))
                .contains("LONG_RUN_3H");
    }

    @Test
    void finishTen_countsFreeRunsToo() {
        Run[] nine = new Run[9];
        java.util.Arrays.fill(nine, run(null));
        assertThat(evaluateWith(nine)).doesNotContain("FINISH_10");

        Run[] ten = new Run[10];
        java.util.Arrays.fill(ten, run(null));
        assertThat(evaluateWith(ten)).contains("FINISH_10");
    }

    @Test
    void distance100km_sumsEveryRun() {
        assertThat(evaluateWith(run("x", "2026-07-09T04:00:00Z", 3600, 60_000),
                run("y", "2026-07-09T04:00:00Z", 3600, 39_999))).doesNotContain("DISTANCE_100KM");
        assertThat(evaluateWith(run("x", "2026-07-09T04:00:00Z", 3600, 60_000),
                run("y", "2026-07-09T04:00:00Z", 3600, 40_000))).contains("DISTANCE_100KM");
    }

    @Test
    void earlyBird_usesKoreanTimeFromMidnight() {
        // 07:59 KST = 전날 22:59 UTC — UTC로 판정하면 이 케이스를 놓친다.
        assertThat(evaluateWith(run("x", "2026-07-08T22:59:00Z", 3600, 5000))).contains("EARLY_BIRD");
        // 00:30 KST(자정 이후) 도 대상
        assertThat(evaluateWith(run("x", "2026-07-08T15:30:00Z", 3600, 5000))).contains("EARLY_BIRD");
        // 08:00 KST 정각은 '8시 이전'이 아니다
        assertThat(evaluateWith(run("x", "2026-07-08T23:00:00Z", 3600, 5000))).doesNotContain("EARLY_BIRD");
    }

    @Test
    void iceCreamRunner_usesKoreanTimeMonth() {
        // 2026-12-01 08:00 KST = 11-30 23:00 UTC — UTC 기준이면 11월로 잘못 본다.
        assertThat(evaluateWith(run("x", "2026-11-30T23:00:00Z", 3600, 5000))).contains("ICE_CREAM_RUNNER");
        assertThat(evaluateWith(run("x", "2026-07-09T04:00:00Z", 3600, 5000))).doesNotContain("ICE_CREAM_RUNNER");
    }

    @Test
    void waypoint3_needsCourseWithThreeOrMoreWaypoints() {
        when(courseRepository.findAllById(any())).thenReturn(List.of(
                Course.builder().id("gunsan-jjamppong-run").waypointCount(2).build()));
        assertThat(evaluateWith(run("gunsan-jjamppong-run"))).doesNotContain("WAYPOINT_3");

        when(courseRepository.findAllById(any())).thenReturn(List.of(
                Course.builder().id("gunsan-jjamppong-run").waypointCount(3).build()));
        assertThat(evaluateWith(run("gunsan-jjamppong-run"))).contains("WAYPOINT_3");
    }

    @Test
    void pioneer_unlocksOnRunWithoutCourse() {
        assertThat(evaluateWith(run("gunsan-jjamppong-run"))).doesNotContain("PIONEER");
        assertThat(evaluateWith(run(null))).contains("PIONEER");
    }

    @Test
    void pendingKinds_neverUnlockAutomatically() {
        // 기준 미확정 5종은 어떤 러닝으로도 자동 달성되면 안 된다(목록에는 나온다).
        List<String> newly = evaluateWith(run("gunsan-jjamppong-run", "2026-12-01T00:00:00Z", 20_000, 200_000));
        assertThat(newly).doesNotContain("SLOW_WALKER", "REST_TIME",
                "JEONJU_CHERRY_BLOSSOM", "JEONJU_DEOKJIN_LAKE", "JEONJU_ECO_MUSEUM");
    }
}
