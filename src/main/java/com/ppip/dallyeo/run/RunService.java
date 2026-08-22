package com.ppip.dallyeo.run;

import com.ppip.dallyeo.achievement.AchievementService;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.CourseRepository;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import com.ppip.dallyeo.run.dto.RunDetailResponse;
import com.ppip.dallyeo.run.dto.RunSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
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

    private final RunRepository runRepository;
    private final CourseRepository courseRepository;
    private final AchievementService achievementService;

    public RunService(RunRepository runRepository, CourseRepository courseRepository,
                      AchievementService achievementService) {
        this.runRepository = runRepository;
        this.courseRepository = courseRepository;
        this.achievementService = achievementService;
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
        // 러닝 저장 시 업적 자동 판정·달성(U6, Q1). 응답 형태는 U5 그대로 유지.
        achievementService.evaluateAndUnlock(userId);
        return toDetail(saved);
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
        Run run = runRepository.findById(id)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "러닝 기록을 찾을 수 없습니다: " + id));
        return toDetail(run);
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
                null,
                run.getStartedAt(),
                run.getFinishedAt());
    }

    private RunSummaryResponse toSummary(Run run) {
        return new RunSummaryResponse(
                run.getId(),
                lookupCourseName(run.getCourseId()),
                run.getDistanceMeters(),
                run.getDurationSeconds(),
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

    /** from(ISO date) → 해당일 00:00:00 UTC. 파싱 실패 → 400. */
    private Instant parseFromDate(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }
        return parseDate(from, "from").atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** to(ISO date) → 해당일 23:59:59.999... UTC(포함). 파싱 실패 → 400. */
    private Instant parseToDate(String to) {
        if (to == null || to.isBlank()) {
            return null;
        }
        return parseDate(to, "to").atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
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
