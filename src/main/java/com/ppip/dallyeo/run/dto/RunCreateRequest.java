package com.ppip.dallyeo.run.dto;

import com.ppip.dallyeo.course.dto.PolylinePoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * 러닝 기록 저장 요청 (US-RUN-1). {@code multipart/form-data} 의 {@code run} 파트로 받는다.
 *
 * <p>경로 전체(polyline)는 받지 않는다 — 클라이언트가 렌더링한 코스 이미지가 대신하므로
 * <b>출발·도착 좌표 2점만</b> 있으면 된다. 좌표는 코스 polyline과 같은 {@code {lat, lng}} 형태다.
 *
 * <p>평균 페이스는 받지 않고 거리·시간에서 서버가 계산한다.
 *
 * @param courseId   시드 코스 참조. 직접 만든 경로면 null. <b>업적 판정(코스·지역·경유지)의 기준</b>이라
 *                   공식 코스를 달렸다면 반드시 보내야 한다.
 * @param startedAt  러닝 시작 시각. 얼리버드 업적 판정에만 쓰이며 없으면 그 업적만 판정하지 않는다.
 * @param finishedAt 러닝 종료 시각 = 기록의 날짜. <b>없으면 서버 저장 시각</b>을 쓴다.
 */
public record RunCreateRequest(
        String courseId,

        @NotNull(message = "start(출발 좌표)는 필수입니다.")
        @Valid
        PolylinePoint start,

        @NotNull(message = "end(도착 좌표)는 필수입니다.")
        @Valid
        PolylinePoint end,

        @NotNull(message = "distanceMeters는 필수입니다.")
        @Positive(message = "distanceMeters는 0보다 커야 합니다.")
        Integer distanceMeters,

        @NotNull(message = "durationSeconds는 필수입니다.")
        @Positive(message = "durationSeconds는 0보다 커야 합니다.")
        Integer durationSeconds,

        Instant startedAt,

        Instant finishedAt
) {
}
