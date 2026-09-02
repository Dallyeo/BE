package com.ppip.dallyeo.badge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * 배지 영속성. 매칭은 정규화 업소명 AND 주소 둘 다 일치(BR-U3-8, 복합 인덱스).
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByNormalizedNameAndNormalizedAddress(String normalizedName, String normalizedAddress);

    /**
     * 목록 화면용 일괄 조회(N+1 방지). 업소명으로만 좁히고 주소 대조는 호출부(메모리)에서 한다 —
     * 복합 인덱스의 선행 컬럼이 normalizedName이라 이 IN 조회가 인덱스를 그대로 탄다.
     */
    List<Badge> findByNormalizedNameIn(Collection<String> normalizedNames);

    boolean existsByTypeAndNormalizedNameAndNormalizedAddress(
            BadgeType type, String normalizedName, String normalizedAddress);
}
