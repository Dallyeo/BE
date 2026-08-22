package com.ppip.dallyeo.course;

import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.course.dto.WaypointAnchor;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.domain.region.RegionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * 기동 시 courses.json 시드 적재 (BR-U2-7, Q4=B).
 * classpath {@code data/courses.json} 로드 → id 기준 <b>최초 1회만 삽입</b>(존재 시 스킵).
 * 한글 region/category → enum 변환, waypointCount 계산. 개별 변환 실패는 WARN+스킵(기동 지속).
 */
@Component
public class CourseDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourseDataLoader.class);
    private static final String RESOURCE = "data/courses.json";

    private final CourseRepository courseRepository;
    private final ObjectMapper objectMapper;

    public CourseDataLoader(CourseRepository courseRepository, ObjectMapper objectMapper) {
        this.courseRepository = courseRepository;
        this.objectMapper = objectMapper;
    }

    /** courses.json 원본 스키마 (한글 region/category). */
    record CourseSeed(
            String id,
            String name,
            String description,
            String region,
            int searchOption,
            String declaredCategory,
            String measuredCategory,
            List<PolylinePoint> polyline,
            List<Integer> cumulativeMeters,
            int totalMeters,
            List<WaypointAnchor> waypointAnchors) {
    }

    @Override
    public void run(ApplicationArguments args) {
        List<CourseSeed> seeds = load();
        int inserted = 0;
        int skipped = 0;
        for (CourseSeed seed : seeds) {
            if (courseRepository.existsById(seed.id())) {
                skipped++;                       // Q4=B: 최초 1회만
                continue;
            }
            Course course = toCourse(seed);
            if (course == null) {
                skipped++;                       // 변환 실패
                continue;
            }
            courseRepository.save(course);
            inserted++;
        }
        log.info("Course seed complete: {} inserted, {} skipped (of {})", inserted, skipped, seeds.size());
    }

    private List<CourseSeed> load() {
        ClassPathResource resource = new ClassPathResource(RESOURCE);
        if (!resource.exists()) {
            log.warn("Course seed resource not found: {} — skipping seed", RESOURCE);
            return List.of();
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<List<CourseSeed>>() {
            });
        } catch (Exception e) {
            log.error("Failed to read course seed {}: {}", RESOURCE, e.toString());
            return List.of();
        }
    }

    /** 한글→enum 변환. 필수 필드(region/measuredCategory) 변환 실패 시 null(스킵). */
    private Course toCourse(CourseSeed seed) {
        Region region = RegionCatalog.fromDisplayName(seed.region());
        CourseDistance measured = CourseDistance.fromKorean(seed.measuredCategory());
        if (region == null || measured == null) {
            log.warn("Skip course id={}: unmapped region='{}' or measuredCategory='{}'",
                    seed.id(), seed.region(), seed.measuredCategory());
            return null;
        }
        CourseDistance declared = CourseDistance.fromKorean(seed.declaredCategory());
        int waypointCount = seed.waypointAnchors() == null ? 0 : seed.waypointAnchors().size();

        return Course.builder()
                .id(seed.id())
                .name(seed.name())
                .description(seed.description())
                .region(region)
                .declaredCategory(declared)
                .measuredCategory(measured)
                .searchOption(seed.searchOption())
                .totalMeters(seed.totalMeters())
                .waypointCount(waypointCount)
                .polyline(seed.polyline())
                .cumulativeMeters(seed.cumulativeMeters())
                .waypointAnchors(seed.waypointAnchors())
                .build();
    }
}
