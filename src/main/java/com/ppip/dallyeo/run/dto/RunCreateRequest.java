package com.ppip.dallyeo.run.dto;

import com.ppip.dallyeo.course.dto.PolylinePoint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

/**
 * 러닝 기록 저장 요청 (US-RUN-1, BR-U5-5). 클라이언트 완료 데이터.
 * courseId는 optional(시드 코스 참조 또는 null). finishedAt >= startedAt 정합은 서비스에서 검증.
 */
public record RunCreateRequest(
        String courseId,

        @NotEmpty(message = "polyline은 비어있을 수 없습니다.")
        List<PolylinePoint> polyline,

        @NotNull(message = "distanceMeters는 필수입니다.")
        @Positive(message = "distanceMeters는 0보다 커야 합니다.")
        Integer distanceMeters,

        @NotNull(message = "durationSeconds는 필수입니다.")
        @Positive(message = "durationSeconds는 0보다 커야 합니다.")
        Integer durationSeconds,

        @NotNull(message = "averagePaceSeconds는 필수입니다.")
        Integer averagePaceSeconds,

        @NotNull(message = "startedAt은 필수입니다.")
        Instant startedAt,

        @NotNull(message = "finishedAt은 필수입니다.")
        Instant finishedAt
) {
}
