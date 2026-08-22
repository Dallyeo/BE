package com.ppip.dallyeo.achievement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 사용자가 달성(unlock)한 업적 기록. (userId, achievement) 유니크로 중복 달성 방지.
 * 카탈로그(고정 8종)는 {@link AchievementType} enum, 여기엔 달성 사실만 저장.
 */
@Entity
@Table(name = "user_achievement", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_achievement", columnNames = {"userId", "achievement"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementType achievement;

    private Instant unlockedAt;

    @PrePersist
    void onCreate() {
        this.unlockedAt = Instant.now();
    }
}
