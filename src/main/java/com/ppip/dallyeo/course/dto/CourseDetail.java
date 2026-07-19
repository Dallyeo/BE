package com.ppip.dallyeo.course.dto;

import com.ppip.dallyeo.course.CourseDistance;
import com.ppip.dallyeo.domain.region.Region;

import java.util.List;

/**
 * 코스 상세 (US-COURSE-2). 경로 좌표/누적거리/경유지 포함(지도 그리기용).
 */
public record CourseDetail(
        String id,
        String name,
        Region region,
        CourseDistance distanceCategory,
        int totalMeters,
        List<PolylinePoint> polyline,
        List<Integer> cumulativeMeters,
        List<WaypointAnchor> waypointAnchors
) {
}
