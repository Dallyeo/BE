package com.ppip.dallyeo.run;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 러닝 기록 영속성 (US-RUN-2). 본인 기록만, 기간(from/to) 선택 필터,
 * finishedAt 내림차순(최신순) 정렬 (BR-U5-9). 인덱스: (userId, finishedAt).
 */
public interface RunRepository extends JpaRepository<Run, Long> {

    @Query("""
            SELECT r FROM Run r
            WHERE r.userId = :userId
              AND (:from IS NULL OR r.finishedAt >= :from)
              AND (:to IS NULL OR r.finishedAt <= :to)
            ORDER BY r.finishedAt DESC
            """)
    List<Run> findByOwnerAndPeriod(@Param("userId") Long userId,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to);

    /** 본인이 완주한(저장한) 코스 id 집합 — 업적 판정용(U6). 자유 러닝(courseId null)은 제외. */
    @Query("SELECT DISTINCT r.courseId FROM Run r WHERE r.userId = :userId AND r.courseId IS NOT NULL")
    List<String> findDistinctCourseIds(@Param("userId") Long userId);
}
