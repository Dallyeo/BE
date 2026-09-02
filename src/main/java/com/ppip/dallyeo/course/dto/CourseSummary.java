package com.ppip.dallyeo.course.dto;

import com.ppip.dallyeo.course.CourseDistance;
import com.ppip.dallyeo.domain.region.Region;

/**
 * 코스 목록 요약 (US-COURSE-1). 경로 데이터 제외. distanceCategory=measuredCategory(Q2=C).
 */
public record CourseSummary(
        String id,
        String name,
        String description,
        String imageUrl,
        Region region,
        CourseDistance distanceCategory,
        int totalMeters,
        int waypointCount
) {
}
