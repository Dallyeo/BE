package com.ppip.dallyeo.achievement.dto;

import java.time.Instant;

/**
 * 업적 목록/달성 응답 (US-ACHV). code=enum 이름(달성 API·프론트 분기용), unlocked=본인 달성 여부.
 * unlockedAt은 미달성 시 null.
 *
 * <p>{@code category}는 화면 상단 지역 탭 분기용 — GUNSAN/JEONJU/<b>COMMON</b>(지역 무관).
 * COMMON이 생겨서 코드 접두사로 지역을 판정할 수 없다.
 * {@code iconOnUrl}(획득=컬러)/{@code iconOffUrl}(미획득=흑백)은 둘 다 내려주고,
 * 프론트가 {@code unlocked} 로 골라 쓴다. 경로는 서버 기준 절대경로다.
 */
public record AchievementResponse(
        String code,
        String category,
        int sortOrder,
        String name,
        String description,
        String iconOnUrl,
        String iconOffUrl,
        boolean unlocked,
        Instant unlockedAt
) {
}
