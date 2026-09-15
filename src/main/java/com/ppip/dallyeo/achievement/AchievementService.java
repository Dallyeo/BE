package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.util.PublicUrlResolver;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.Course;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.run.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 업적 판정/조회 (US-ACHV, Q1=둘 다 / Q2=courseId 기반).
 * 카탈로그는 21종({@link AchievementType}) — 판정 기준이 정해진 것만 자동 달성하고,
 * 기준 미확정({@link AchievementType.Kind#PENDING})은 목록에만 나온다.
 * - 자동: 러닝 저장 시 {@link #evaluateAndUnlock(Long)} 호출로 조건 충족 업적 자동 달성.
 * - 수동: {@link #unlock(Long, String)} 로 특정 업적 재판정 후 달성.
 * - 조회: {@link #list(Long)} 전체 21종 + 본인 달성 여부/일시(시안 정렬 순).
 * 판정 기준: 본인이 저장한 run의 distinct courseId 집합. 지역 코스 집합은 course 테이블 실데이터로 해석.
 */
@Service
@Transactional(readOnly = true)
public class AchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final RunRepository runRepository;
    private final CourseRepository courseRepository;
    private final PublicUrlResolver urls;

    public AchievementService(UserAchievementRepository userAchievementRepository,
                              RunRepository runRepository,
                              CourseRepository courseRepository,
                              PublicUrlResolver urls) {
        this.userAchievementRepository = userAchievementRepository;
        this.runRepository = runRepository;
        this.courseRepository = courseRepository;
        this.urls = urls;
    }

    /**
     * 러닝 저장 후 자동 판정 — 새로 충족한 <b>미달성</b> 업적만 저장하고 그 목록을 반환.
     *
     * <p>반환값이 러닝 결과창의 도장으로 그대로 쓰인다. 이미 달성한 업적은
     * {@code already} 에서 걸러지므로 <b>같은 조건을 다시 채워도 두 번 다시 나오지 않는다</b>
     * (DB에도 {@code uk_user_achievement} 유니크로 이중 방어). 결과창에서만 보여야 하므로
     * 러닝 조회({@code GET /runs/{id}})에는 싣지 않는다 — RunService 참고.
     */
    @Transactional
    public List<AchievementResponse> evaluateAndUnlock(Long userId) {
        RunHistory history = RunHistory.of(runRepository.findByOwnerAndPeriod(userId, null, null));
        Map<Region, Set<String>> regionCourses = new EnumMap<>(Region.class);
        Set<AchievementType> already = currentlyUnlocked(userId);

        List<AchievementResponse> newlyUnlocked = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            if (already.contains(type)) {
                continue;   // 최초 달성에서만 도장 — 재달성은 조용히 무시
            }
            if (isSatisfied(type, history, regionCourses)) {
                UserAchievement saved = userAchievementRepository.save(UserAchievement.builder()
                        .userId(userId).achievement(type).build());
                newlyUnlocked.add(toResponse(type, true, saved.getUnlockedAt()));
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
        RunHistory history = RunHistory.of(runRepository.findByOwnerAndPeriod(userId, null, null));
        if (!isSatisfied(type, history, new EnumMap<>(Region.class))) {
            throw new BusinessException(ErrorCode.CONFLICT, "업적 달성 조건을 충족하지 않았습니다: " + type.displayName());
        }
        UserAchievement saved = userAchievementRepository.save(UserAchievement.builder()
                .userId(userId).achievement(type).build());
        return toResponse(type, true, saved.getUnlockedAt());
    }

    /** 전체 업적 21종 + 본인 달성 여부/일시. 정렬은 시안 순서(sortOrder). */
    public List<AchievementResponse> list(Long userId) {
        Map<AchievementType, java.time.Instant> unlockedAt = new EnumMap<>(AchievementType.class);
        for (UserAchievement ua : userAchievementRepository.findByUserId(userId)) {
            unlockedAt.put(ua.getAchievement(), ua.getUnlockedAt());
        }
        List<AchievementResponse> result = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            result.add(toResponse(type, unlockedAt.containsKey(type), unlockedAt.get(type)));
        }
        result.sort(Comparator.comparingInt(AchievementResponse::sortOrder));   // 시안 순서(군산→전주→공통)
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
    private boolean isSatisfied(AchievementType type, RunHistory history,
                                Map<Region, Set<String>> regionCoursesCache) {
        Set<String> userCourseIds = history.courseIds();
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
            case SINGLE_DURATION_SECONDS -> history.longestDurationSeconds() > type.threshold();
            case RUN_COUNT -> history.runCount() >= type.threshold();
            case TOTAL_DISTANCE_METERS -> history.totalDistanceMeters() >= type.threshold();
            case FINISH_IN_MONTH -> history.finishedInMonth(type.threshold());
            case START_BEFORE_HOUR -> history.startedBefore(type.threshold());
            case COURSE_WAYPOINTS -> hasCourseWithWaypoints(userCourseIds, type.threshold());
            case FREE_RUN -> history.hasFreeRun();
            // 판정 기준 미확정 — 목록에는 나오되 자동 달성하지 않는다.
            case PENDING -> false;
        };
    }

    /** 완주한 코스 중 경유지가 {@code min}개 이상인 것이 있는지. */
    private boolean hasCourseWithWaypoints(Set<String> userCourseIds, int min) {
        if (userCourseIds.isEmpty()) {
            return false;
        }
        return courseRepository.findAllById(userCourseIds).stream()
                .anyMatch(c -> c.getWaypointCount() >= min);
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
                type.name(), type.category().name(), type.sortOrder(),
                type.displayName(), type.description(),
                urls.absolute(type.iconOnUrl()), urls.absolute(type.iconOffUrl()),
                unlocked, unlockedAt);
    }
}
