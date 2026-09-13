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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    /**
     * VARCHAR로 고정한다. 그냥 두면 Hibernate MySQL 방언이 네이티브 {@code enum('A','B',…)} 컬럼을
     * 만드는데, {@code ddl-auto=update}는 <b>기존 컬럼 정의를 바꾸지 않아</b> 업적을 추가해도
     * 컬럼은 옛 목록 그대로다 → 새 코드 저장 시 "Data truncated" 로 실패한다(실제로 겪음).
     * VARCHAR면 카탈로그가 늘어나도 DDL 변경이 필요 없다.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private AchievementType achievement;

    private Instant unlockedAt;

    @PrePersist
    void onCreate() {
        this.unlockedAt = Instant.now();
    }
}
