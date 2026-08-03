package com.ppip.dallyeo.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 사용자 엔티티 (US-USER-1/2/3, domain-entities). 소셜 전용(패스워드 없음).
 * 재로그인 매칭 키 = (provider, providerUserId) 복합 유니크(Q3=A).
 * 삭제는 하드 삭제(Q7=A) — soft-delete 컬럼 없음.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_provider", columnNames = {"provider", "providerUserId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    /** 소셜 고유 ID(카카오 회원번호 / Apple sub). */
    @Column(nullable = false)
    private String providerUserId;

    /** 소셜 닉네임 또는 자동생성(러너####). non-null(BR-5.2/5.3). */
    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    /** cm. 온보딩 미입력 시 null. */
    private Double height;

    /** kg. 온보딩 미입력 시 null. */
    private Double weight;

    /** 저장방식 미정(보류 백로그) — 항상 null. */
    private String profileImageUrl;

    /** 온보딩 완료 플래그(Q8=B). onboardingRequired = !onboardingCompleted. */
    @Column(nullable = false)
    private boolean onboardingCompleted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.gender == null) {
            this.gender = Gender.NONE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
