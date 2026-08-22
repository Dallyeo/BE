package com.ppip.dallyeo.run;

import com.ppip.dallyeo.course.converter.PolylineConverter;
import com.ppip.dallyeo.course.dto.PolylinePoint;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * 러닝 기록 엔티티 (US-RUN-1/2/3, domain-entities). 클라이언트가 추적한 완료 데이터의 스냅샷.
 * 소유자 = userId(스칼라, FK 제약 없음). courseId는 시드 코스 느슨 참조(nullable, 검증 없음 — BR-U5-6).
 * polyline은 JSON TEXT 컬럼(U2 PolylineConverter 재사용) — 상한 미설정이라 MEDIUMTEXT로 절단 방지(NFR Design 2.3).
 * completionRate는 저장하지 않음(계산 기준 보류 — BR-U5-8).
 */
@Entity
@Table(name = "run", indexes = {
        @Index(name = "idx_run_user_finished", columnList = "userId, finishedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Run {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자(토큰 사용자). 소유권 검증 키. */
    @Column(nullable = false)
    private Long userId;

    /** 달린 시드 코스 참조. 자유 러닝/직접 만든 경로면 null. FK 제약·존재검증 없음. */
    private String courseId;

    /** 실제 달린 경로 좌표. 상한 미설정 → MEDIUMTEXT(≈16MB)로 절단 방지. */
    @Convert(converter = PolylineConverter.class)
    @Column(columnDefinition = "MEDIUMTEXT")
    private List<PolylinePoint> polyline;

    private int distanceMeters;

    private int durationSeconds;

    /** 평균 페이스(초/km). 클라이언트 계산값 그대로 저장(BR-U5-7). */
    private int averagePaceSeconds;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
