package com.ppip.dallyeo.course;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.course.dto.CourseDetail;
import com.ppip.dallyeo.course.dto.CourseSummary;
import com.ppip.dallyeo.domain.region.Region;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 코스 조회 API (US-COURSE-1/2). 공개(permitAll).
 * region/distance 필터는 선택. 잘못된 값 → 400 (BR-U2-4).
 */
@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseQueryService courseQueryService;

    public CourseController(CourseQueryService courseQueryService) {
        this.courseQueryService = courseQueryService;
    }

    @GetMapping
    public ApiResponse<List<CourseSummary>> list(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String distance) {
        Region regionFilter = parseRegion(region);
        CourseDistance distanceFilter = parseDistance(distance);
        return ApiResponse.success(courseQueryService.findSummaries(regionFilter, distanceFilter));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseDetail> detail(@PathVariable String id) {
        return ApiResponse.success(courseQueryService.findDetail(id));
    }

    private Region parseRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        try {
            return Region.valueOf(region.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 지역입니다: " + region);
        }
    }

    private CourseDistance parseDistance(String distance) {
        if (distance == null || distance.isBlank()) {
            return null;
        }
        CourseDistance parsed = CourseDistance.fromParam(distance);
        if (parsed == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 거리 분류입니다: " + distance);
        }
        return parsed;
    }
}
