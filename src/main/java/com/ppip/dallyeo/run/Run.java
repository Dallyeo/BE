package com.ppip.dallyeo.run;

import jakarta.persistence.Column;
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

/**
 * 러닝 기록 엔티티 (US-RUN-1/2/3, domain-entities). 클라이언트가 추적한 완료 데이터의 스냅샷.
 * 소유자 = userId(스칼라, FK 제약 없음). courseId는 시드 코스 느슨 참조(nullable, 검증 없음 — BR-U5-6).
 * completionRate는 저장하지 않음(계산 기준 보류 — BR-U5-8).
 *
 * <p>경로는 <b>출발·도착 좌표 2점만</b> 남긴다. 전체 경로는 클라이언트가 렌더링한
 * 코스 이미지({@code imageUrl})가 대신하므로 좌표 배열을 보관하지 않는다.
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

    /** 출발 지점. */
    @Column(nullable = false)
    private double startLat;

    @Column(nullable = false)
    private double startLng;

    /** 도착 지점. */
    @Column(nullable = false)
    private double endLat;

    @Column(nullable = false)
    private double endLng;

    private int distanceMeters;

    private int durationSeconds;

    /** 평균 페이스(초/km). 거리·시간에서 서버가 계산한다(클라이언트 값을 받지 않는다). */
    private int averagePaceSeconds;

    /** 기록 이미지 공개 URL 경로(예: /uploads/runs/{uuid}.jpg). 저장 시 필수. */
    @Column(nullable = false)
    private String imageUrl;

    /** 러닝 시작 시각. 클라이언트가 주면 그 값, 없으면 null(얼리버드 업적 판정에만 쓰인다). */
    private Instant startedAt;

    /**
     * 러닝 날짜(종료 시각). 클라이언트가 주면 그 값, <b>없으면 저장 시각</b>.
     * 목록 정렬·기간 필터·월 판정의 기준이라 항상 값이 있어야 한다.
     */
    @Column(nullable = false)
    private Instant finishedAt;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
