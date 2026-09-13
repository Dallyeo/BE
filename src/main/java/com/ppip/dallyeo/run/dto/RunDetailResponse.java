package com.ppip.dallyeo.run.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.course.dto.PolylinePoint;

import java.time.Instant;
import java.util.List;

/**
 * 러닝 기록 상세/저장 응답 (US-RUN-1/3). polyline 포함.
 * completionRate는 이번 유닛 미계산 → 항상 null (BR-U5-8, ApiResponse NON_NULL로 생략 가능).
 *
 * <p>{@code newAchievements}는 <b>러닝 저장(POST /runs) 응답에서만</b> 채워진다 —
 * 그 러닝으로 <b>처음</b> 달성한 업적이고, 결과창에 띄울 도장이다. 조회(GET)에서는 null이라
 * 직렬화에서 아예 빠지므로, 지난 기록을 다시 열어도 도장이 재생되지 않는다.
 * 새로 달성한 업적이 없으면 빈 배열이다(필드 자체는 존재).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunDetailResponse(
        Long id,
        String courseId,
        String courseName,
        List<PolylinePoint> polyline,
        int distanceMeters,
        int durationSeconds,
        int averagePaceSeconds,
        String imageUrl,
        Double completionRate,
        Instant startedAt,
        Instant finishedAt,
        List<AchievementResponse> newAchievements
) {

    /** 조회 응답용 — 도장은 저장 응답에만 실린다. */
    public RunDetailResponse withoutAchievements() {
        return new RunDetailResponse(id, courseId, courseName, polyline, distanceMeters,
                durationSeconds, averagePaceSeconds, imageUrl, completionRate,
                startedAt, finishedAt, null);
    }

    /** 저장 응답용 — 이번에 처음 달성한 업적을 도장으로 싣는다. */
    public RunDetailResponse withAchievements(List<AchievementResponse> newAchievements) {
        return new RunDetailResponse(id, courseId, courseName, polyline, distanceMeters,
                durationSeconds, averagePaceSeconds, imageUrl, completionRate,
                startedAt, finishedAt, newAchievements);
    }
}
