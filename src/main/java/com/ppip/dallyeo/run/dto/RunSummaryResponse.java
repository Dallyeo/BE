package com.ppip.dallyeo.run.dto;

import java.time.Instant;

/**
 * 러닝 기록 목록 요약 (US-RUN-2). 경량 — polyline 미포함(NFR Design 2.2).
 * courseName은 courseId 시드 코스 best-effort lookup(없으면 null).
 */
public record RunSummaryResponse(
        Long id,
        String courseName,
        int distanceMeters,
        int durationSeconds,
        Instant finishedAt
) {
}
