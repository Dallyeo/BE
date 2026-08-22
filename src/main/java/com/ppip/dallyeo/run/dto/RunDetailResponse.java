package com.ppip.dallyeo.run.dto;

import com.ppip.dallyeo.course.dto.PolylinePoint;

import java.time.Instant;
import java.util.List;

/**
 * 러닝 기록 상세/저장 응답 (US-RUN-1/3). polyline 포함.
 * completionRate는 이번 유닛 미계산 → 항상 null (BR-U5-8, ApiResponse NON_NULL로 생략 가능).
 */
public record RunDetailResponse(
        Long id,
        String courseId,
        String courseName,
        List<PolylinePoint> polyline,
        int distanceMeters,
        int durationSeconds,
        int averagePaceSeconds,
        Double completionRate,
        Instant startedAt,
        Instant finishedAt
) {
}
