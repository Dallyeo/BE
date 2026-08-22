package com.ppip.dallyeo.achievement.dto;

import java.time.Instant;

/**
 * 업적 목록/달성 응답 (US-ACHV). code=enum 이름(달성 API·프론트 분기용), unlocked=본인 달성 여부.
 * unlockedAt은 미달성 시 null.
 */
public record AchievementResponse(
        String code,
        String name,
        String description,
        boolean unlocked,
        Instant unlockedAt
) {
}
