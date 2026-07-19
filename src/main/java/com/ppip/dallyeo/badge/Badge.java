package com.ppip.dallyeo.badge;

import com.ppip.dallyeo.domain.region.Region;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 배지 엔티티 (US-BADGE-1). CSV 적재 결과. 매칭 키는 (normalizedName, normalizedAddress) 둘 다(BR-U3-8).
 * U3 소유. 현재 군산만 적재(P6).
 */
@Entity
@Table(name = "badge", indexes = {
        @Index(name = "idx_badge_match", columnList = "normalizedName,normalizedAddress")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private BadgeType type;

    @Enumerated(EnumType.STRING)
    private Region region;

    private String placeName;          // 원본 업소명
    private String normalizedName;     // 정규화 업소명(매칭 키)
    private String normalizedAddress;  // 정규화 주소(매칭 키)
}
