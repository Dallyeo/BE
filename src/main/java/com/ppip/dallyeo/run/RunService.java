package com.ppip.dallyeo.run;

import com.ppip.dallyeo.achievement.AchievementService;
import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.storage.ImageStorage;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import com.ppip.dallyeo.run.dto.RunDetailResponse;
import com.ppip.dallyeo.run.dto.RunSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 러닝 기록 저장/조회 (US-RUN-1/2/3). 전부 인증 소유자 전용.
 * - 저장: 검증(BR-U5-5) 후 userId 귀속. courseId 느슨(존재검증 없음 — BR-U5-6).
 * - 목록: 본인 격리 + 기간 필터(finishedAt 기준, 최신순) — polyline 제외 경량.
 * - 상세: 소유권 검증(타인/미존재 → 404 은폐, BR-U5-2).
 * completionRate는 미계산(null, BR-U5-8). courseName은 시드 코스 best-effort lookup.
 */
@Service
@Transactional(readOnly = true)
public class RunService {

    /** 기록 이미지 저장 하위 디렉터리 — {dir}/runs/. */
    private static final String IMAGE_CATEGORY = "runs";

    private final RunRepository runRepository;
    private final CourseRepository courseRepository;
    private final AchievementService achievementService;
    private final ImageStorage imageStorage;

    public RunService(RunRepository runRepository, CourseRepository courseRepository,
                      AchievementService achievementService, ImageStorage imageStorage) {
        this.runRepository = runRepository;
        this.courseRepository = courseRepository;
        this.achievementService = achievementService;
        this.imageStorage = imageStorage;
    }

    /** 러닝 기록 저장 (US-RUN-1). 검증 위반 → 400. */
    @Transactional
    public RunDetailResponse save(Long userId, RunCreateRequest request) {
        validate(request);
        Run run = Run.builder()
                .userId(userId)
                .courseId(request.courseId())
                .polyline(request.polyline())
                .distanceMeters(request.distanceMeters())
                .durationSeconds(request.durationSeconds())
                .averagePaceSeconds(request.averagePaceSeconds())
                .startedAt(request.startedAt())
                .finishedAt(request.finishedAt())
                .build();
        Run saved = runRepository.save(run);
        // 러닝 저장 시 업적 자동 판정·달성(U6, Q1).
        // 이번에 "처음" 달성한 업적만 돌아오며, 이걸 결과창 도장으로 응답에 싣는다.
        // 재달성은 여기서 이미 걸러지므로 도장이 두 번 뜨지 않는다.
        List<AchievementResponse> newAchievements = achievementService.evaluateAndUnlock(userId);
        return toDetail(saved).withAchievements(newAchievements);
    }

    /** 러닝 기록 목록 (US-RUN-2). 본인만, 기간(ISO date) 선택, 최신순. */
    public List<RunSummaryResponse> list(Long userId, String from, String to) {
        Instant fromInstant = parseFromDate(from);
        Instant toInstant = parseToDate(to);
        return runRepository.findByOwnerAndPeriod(userId, fromInstant, toInstant).stream()
                .map(this::toSummary)
                .toList();
    }

    /** 러닝 기록 상세 (US-RUN-3). 미존재/타인 → 404. */
    public RunDetailResponse getDetail(Long userId, Long id) {
        return toDetail(findOwned(userId, id));
    }

    /**
     * 기록 이미지 업로드/교체. 미존재/타인 → 404(소유권 은폐, BR-U5-2).
     * 이미 이미지가 있으면 새 파일로 교체하고 이전 파일은 지운다.
     */
    @Transactional
    public RunDetailResponse attachImage(Long userId, Long id, MultipartFile file) {
        Run run = findOwned(userId, id);
        String previous = run.getImageUrl();
        run.setImageUrl(imageStorage.store(file, IMAGE_CATEGORY));
        Run saved = runRepository.save(run);
        if (previous != null && !previous.equals(saved.getImageUrl())) {
            imageStorage.deleteQuietly(previous);
        }
        return toDetail(saved);
    }

    /** 본인 소유 기록 조회. 미존재/타인 모두 404로 동일 처리(존재 여부 노출 방지). */
    private Run findOwned(Long userId, Long id) {
        return runRepository.findById(id)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "러닝 기록을 찾을 수 없습니다: " + id));
    }

    private void validate(RunCreateRequest request) {
        if (request.finishedAt().isBefore(request.startedAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "finishedAt은 startedAt보다 앞설 수 없습니다.");
        }
    }

    private RunDetailResponse toDetail(Run run) {
        return new RunDetailResponse(
                run.getId(),
                run.getCourseId(),
                lookupCourseName(run.getCourseId()),
                run.getPolyline(),
                run.getDistanceMeters(),
                run.getDurationSeconds(),
                run.getAveragePaceSeconds(),
                run.getImageUrl(),
                null,
                run.getStartedAt(),
                run.getFinishedAt(),
                null);   // 도장은 저장 응답에서만 — withAchievements 참고
    }

    private RunSummaryResponse toSummary(Run run) {
        return new RunSummaryResponse(
                run.getId(),
                lookupCourseName(run.getCourseId()),
                run.getDistanceMeters(),
                run.getDurationSeconds(),
                run.getImageUrl(),
                run.getFinishedAt());
    }

    /** 시드 코스명 best-effort. courseId가 null이거나 코스가 없으면 null. */
    private String lookupCourseName(String courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findById(courseId)
                .map(c -> c.getName())
                .orElse(null);
    }

    /** from(ISO date) → 해당일 00:00:00 한국시간. 파싱 실패 → 400. */
    /** 날짜 경계는 한국시간 기준 — 사용자가 화면에서 보는 '날짜'와 어긋나면 안 된다. */
    private static final ZoneId KST = ZoneId.of(com.ppip.dallyeo.DallyeoApplication.ZONE);

    private Instant parseFromDate(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }
        return parseDate(from, "from").atStartOfDay(KST).toInstant();
    }

    /** to(ISO date) → 해당일 23:59:59.999... 한국시간(포함). 파싱 실패 → 400. */
    private Instant parseToDate(String to) {
        if (to == null || to.isBlank()) {
            return null;
        }
        return parseDate(to, "to").atTime(LocalTime.MAX).atZone(KST).toInstant();
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    field + "는 ISO date(YYYY-MM-DD) 형식이어야 합니다: " + value);
        }
    }
}
