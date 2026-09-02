package com.ppip.dallyeo.course;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.dto.CourseDetail;
import com.ppip.dallyeo.course.dto.CourseSummary;
import com.ppip.dallyeo.domain.region.Region;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 코스 조회 (US-COURSE-1/2). 목록=요약(필터/정렬), 상세=경로 포함/404.
 * distanceCategory 응답/필터 기준은 measuredCategory (Q2=C).
 */
@Service
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;

    public CourseQueryService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /** 목록 조회: region/distance 선택 필터(null=미적용), totalMeters ASC. */
    public List<CourseSummary> findSummaries(Region region, CourseDistance distance) {
        return courseRepository.findByFilters(region, distance).stream()
                .map(this::toSummary)
                .toList();
    }

    /** 상세 조회: 미존재 → 404 NOT_FOUND (BR-U2-6). */
    public CourseDetail findDetail(String id) {
        Course c = courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "코스를 찾을 수 없습니다: " + id));
        return new CourseDetail(
                c.getId(), c.getName(), c.getDescription(), c.getImageUrl(),
                c.getRegion(), c.getMeasuredCategory(),
                c.getTotalMeters(), c.getPolyline(), c.getCumulativeMeters(), c.getWaypointAnchors());
    }

    private CourseSummary toSummary(Course c) {
        return new CourseSummary(
                c.getId(), c.getName(), c.getDescription(), c.getImageUrl(),
                c.getRegion(), c.getMeasuredCategory(),
                c.getTotalMeters(), c.getWaypointCount());
    }
}
