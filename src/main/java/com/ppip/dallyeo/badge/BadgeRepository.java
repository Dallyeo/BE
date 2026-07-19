package com.ppip.dallyeo.badge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 배지 영속성. 매칭은 정규화 업소명 AND 주소 둘 다 일치(BR-U3-8, 복합 인덱스).
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByNormalizedNameAndNormalizedAddress(String normalizedName, String normalizedAddress);

    boolean existsByTypeAndNormalizedNameAndNormalizedAddress(
            BadgeType type, String normalizedName, String normalizedAddress);
}
