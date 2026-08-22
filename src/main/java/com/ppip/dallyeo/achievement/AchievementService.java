package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.Course;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.run.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 업적 판정/조회 (US-ACHV, Q1=둘 다 / Q2=courseId 기반).
 * - 자동: 러닝 저장 시 {@link #evaluateAndUnlock(Long)} 호출로 조건 충족 업적 자동 달성.
 * - 수동: {@link #unlock(Long, String)} 로 특정 업적 재판정 후 달성.
 * - 조회: {@link #list(Long)} 전체 8종 + 본인 달성 여부/일시.
 * 판정 기준: 본인이 저장한 run의 distinct courseId 집합. 지역 코스 집합은 course 테이블 실데이터로 해석.
 */
@Service
@Transactional(readOnly = true)
public class AchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final RunRepository runRepository;
    private final CourseRepository courseRepository;

    public AchievementService(UserAchievementRepository userAchievementRepository,
                              RunRepository runRepository,
                              CourseRepository courseRepository) {
        this.userAchievementRepository = userAchievementRepository;
        this.runRepository = runRepository;
        this.courseRepository = courseRepository;
    }

    /** 러닝 저장 후 자동 판정 — 새로 충족한 미달성 업적을 저장하고 그 목록을 반환. */
    @Transactional
    public List<AchievementType> evaluateAndUnlock(Long userId) {
        Set<String> userCourseIds = new HashSet<>(runRepository.findDistinctCourseIds(userId));
        Map<Region, Set<String>> regionCourses = new EnumMap<>(Region.class);
        Set<AchievementType> already = currentlyUnlocked(userId);

        List<AchievementType> newlyUnlocked = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            if (already.contains(type)) {
                continue;
            }
            if (isSatisfied(type, userCourseIds, regionCourses)) {
                userAchievementRepository.save(UserAchievement.builder()
                        .userId(userId).achievement(type).build());
                newlyUnlocked.add(type);
            }
        }
        return newlyUnlocked;
    }

    /** 특정 업적 수동 달성. 미지원 코드 → 404, 조건 미충족 → 409, 이미 달성 → 기존 응답(멱등). */
    @Transactional
    public AchievementResponse unlock(Long userId, String code) {
        AchievementType type = AchievementType.fromCode(code);
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "존재하지 않는 업적입니다: " + code);
        }
        if (userAchievementRepository.existsByUserIdAndAchievement(userId, type)) {
            return toResponse(type, true, findUnlockedAt(userId, type));
        }
        Set<String> userCourseIds = new HashSet<>(runRepository.findDistinctCourseIds(userId));
        if (!isSatisfied(type, userCourseIds, new EnumMap<>(Region.class))) {
            throw new BusinessException(ErrorCode.CONFLICT, "업적 달성 조건을 충족하지 않았습니다: " + type.displayName());
        }
        UserAchievement saved = userAchievementRepository.save(UserAchievement.builder()
                .userId(userId).achievement(type).build());
        return toResponse(type, true, saved.getUnlockedAt());
    }

    /** 전체 업적 8종 + 본인 달성 여부/일시. */
    public List<AchievementResponse> list(Long userId) {
        Map<AchievementType, java.time.Instant> unlockedAt = new EnumMap<>(AchievementType.class);
        for (UserAchievement ua : userAchievementRepository.findByUserId(userId)) {
            unlockedAt.put(ua.getAchievement(), ua.getUnlockedAt());
        }
        List<AchievementResponse> result = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            result.add(toResponse(type, unlockedAt.containsKey(type), unlockedAt.get(type)));
        }
        return result;
    }

    private Set<AchievementType> currentlyUnlocked(Long userId) {
        Set<AchievementType> set = new HashSet<>();
        for (UserAchievement ua : userAchievementRepository.findByUserId(userId)) {
            set.add(ua.getAchievement());
        }
        return set;
    }

    private java.time.Instant findUnlockedAt(Long userId, AchievementType type) {
        for (UserAchievement ua : userAchievementRepository.findByUserId(userId)) {
            if (ua.getAchievement() == type) {
                return ua.getUnlockedAt();
            }
        }
        return null;
    }

    /** 조건 판정. regionCourses는 호출 내 캐시(지역→코스id 집합). */
    private boolean isSatisfied(AchievementType type, Set<String> userCourseIds,
                                Map<Region, Set<String>> regionCoursesCache) {
        return switch (type.kind()) {
            case COURSE -> userCourseIds.contains(type.courseId());
            case REGION_ANY -> {
                Set<String> ids = regionCourseIds(type.region(), regionCoursesCache);
                yield ids.stream().anyMatch(userCourseIds::contains);
            }
            case REGION_ALL -> {
                Set<String> ids = regionCourseIds(type.region(), regionCoursesCache);
                yield !ids.isEmpty() && userCourseIds.containsAll(ids);
            }
        };
    }

    private Set<String> regionCourseIds(Region region, Map<Region, Set<String>> cache) {
        return cache.computeIfAbsent(region, r -> {
            Set<String> ids = new HashSet<>();
            for (Course c : courseRepository.findByFilters(r, null)) {
                ids.add(c.getId());
            }
            return ids;
        });
    }

    private AchievementResponse toResponse(AchievementType type, boolean unlocked, java.time.Instant unlockedAt) {
        return new AchievementResponse(
                type.name(), type.displayName(), type.description(),
                unlocked, unlockedAt);
    }
}
