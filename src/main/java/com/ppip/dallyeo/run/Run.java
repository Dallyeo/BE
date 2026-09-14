package com.ppip.dallyeo.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * 러닝 기록 엔티티 (US-RUN-1/2/3, domain-entities). 클라이언트가 추적한 완료 데이터의 스냅샷.
 * 소유자 = userId(스칼라, FK 제약 없음). courseId는 시드 코스 느슨 참조(nullable, 검증 없음 — BR-U5-6).
 * completionRate는 저장하지 않음(계산 기준 보류 — BR-U5-8).
 *
 * <p>경로는 <b>출발·도착 좌표 2점만</b> 남긴다. 전체 경로는 클라이언트가 렌더링한
 * 코스 이미지({@code imageUrl})가 대신하므로 좌표 배열을 보관하지 않는다.
 */
@Entity
@Table(name = "run",
        indexes = { @Index(name = "idx_run_user_finished", columnList = "userId, finishedAt") },
        uniqueConstraints = {
                // 같은 사용자가 같은 clientRunId 로 두 번 저장하지 못하게. NULL 은 MySQL 에서
                // 서로 다른 값으로 취급되므로, 키를 안 보낸 기록은 제약에 걸리지 않는다.
                @UniqueConstraint(name = "uk_run_user_client", columnNames = {"userId", "clientRunId"})
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

    /**
     * 클라이언트가 만든 중복 방지 키(멱등키). 선택.
     *
     * <p>저장은 됐는데 응답이 유실되면 클라이언트는 성공/실패를 구분할 수 없어 재전송한다.
     * 그때 같은 키가 오면 새로 만들지 않고 기존 기록을 돌려준다. 특히 비로그인 러닝을
     * 가입 후 몰아서 올리는 경로에서 재시도가 잦다.
     *
     * <p>중복을 사후에 지울 방법이 없고(러닝 삭제 API 없음), 누적 업적(완주 10회·100km)이
     * 부풀려지면 <b>되돌릴 수 없으므로</b> 서버에서 막는다.
     */
    @Column(length = 64)
    private String clientRunId;

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

    /**
     * 소모 칼로리(kcal). 클라이언트가 준 값을 그대로 보관하며, 없으면 null.
     *
     * <p>페이스와 달리 서버가 계산하지 않는다 — iOS 는 HealthKit 에서 심박·모션까지 반영된 값을
     * 얻을 수 있어, 서버가 체중·거리·시간만으로 추정하는 것보다 정확하다.
     * (서버의 {@code User.weight} 는 비어 있을 수 있어 추정 자체가 불가능한 경우도 있다.)
     */
    private Integer calories;

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
