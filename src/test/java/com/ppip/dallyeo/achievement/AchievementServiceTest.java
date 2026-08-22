package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.Course;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.run.RunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    void stubRegions() {
        when(courseRepository.findByFilters(Region.GUNSAN, null)).thenReturn(courses(GUNSAN_IDS, Region.GUNSAN));
        when(courseRepository.findByFilters(Region.JEONJU, null)).thenReturn(courses(JEONJU_IDS, Region.JEONJU));
    }

    @Test
    void evaluate_courseAndRegionAny_unlockedButNotConqueror() {
        when(runRepository.findDistinctCourseIds(1L)).thenReturn(List.of("gunsan-jjamppong-run"));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<AchievementType> newly = service.evaluateAndUnlock(1L);

        assertThat(newly).contains(AchievementType.JJAMPPONG, AchievementType.GUNSAN_BEGINNER);
        assertThat(newly).doesNotContain(AchievementType.GUNSAN_CONQUEROR, AchievementType.JEONJU_BEGINNER);
        verify(userAchievementRepository, never()).save(argThat(ua -> ua.getAchievement() == AchievementType.GUNSAN_CONQUEROR));
    }

    @Test
    void evaluate_allGunsanCourses_unlocksConqueror() {
        when(runRepository.findDistinctCourseIds(1L)).thenReturn(GUNSAN_IDS);
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        List<AchievementType> newly = service.evaluateAndUnlock(1L);

        assertThat(newly).contains(AchievementType.GUNSAN_CONQUEROR, AchievementType.GUNSAN_BEGINNER,
                AchievementType.JJAMPPONG, AchievementType.NATURE_LOVER, AchievementType.BETWEEN_WAVES);
    }

    @Test
    void evaluate_skipsAlreadyUnlocked() {
        when(runRepository.findDistinctCourseIds(1L)).thenReturn(List.of("gunsan-jjamppong-run"));
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(
                UserAchievement.builder().userId(1L).achievement(AchievementType.JJAMPPONG).build()));

        List<AchievementType> newly = service.evaluateAndUnlock(1L);

        assertThat(newly).doesNotContain(AchievementType.JJAMPPONG);
        assertThat(newly).contains(AchievementType.GUNSAN_BEGINNER);
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
        when(runRepository.findDistinctCourseIds(1L)).thenReturn(List.of("gunsan-wetland-eco-run"));

        assertThatThrownBy(() -> service.unlock(1L, "JJAMPPONG"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CONFLICT);
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void unlock_conditionMet_saves() {
        when(userAchievementRepository.existsByUserIdAndAchievement(1L, AchievementType.JJAMPPONG)).thenReturn(false);
        when(runRepository.findDistinctCourseIds(1L)).thenReturn(List.of("gunsan-jjamppong-run"));
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
    void list_returnsAllEightWithUnlockedFlags() {
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of(
                UserAchievement.builder().userId(1L).achievement(AchievementType.JJAMPPONG).build()));

        List<AchievementResponse> res = service.list(1L);

        assertThat(res).hasSize(8);
        assertThat(res).anyMatch(a -> a.code().equals("JJAMPPONG") && a.unlocked());
        assertThat(res).anyMatch(a -> a.code().equals("GUNSAN_CONQUEROR") && !a.unlocked());
    }
}
