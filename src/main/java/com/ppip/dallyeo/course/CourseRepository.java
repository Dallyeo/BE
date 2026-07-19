package com.ppip.dallyeo.course;

import com.ppip.dallyeo.domain.region.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 코스 영속성 (US-COURSE-1/2). region/measuredCategory 필터는 선택(null=미적용),
 * 정렬은 totalMeters ASC (BR-U2-5). 인덱스: region, measuredCategory (R2).
 */
public interface CourseRepository extends JpaRepository<Course, String> {

    @Query("""
            SELECT c FROM Course c
            WHERE (:region IS NULL OR c.region = :region)
              AND (:distance IS NULL OR c.measuredCategory = :distance)
            ORDER BY c.totalMeters ASC
            """)
    List<Course> findByFilters(@Param("region") Region region,
                               @Param("distance") CourseDistance distance);
}
