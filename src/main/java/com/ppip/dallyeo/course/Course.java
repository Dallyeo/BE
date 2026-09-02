package com.ppip.dallyeo.course;

import com.ppip.dallyeo.course.converter.IntListConverter;
import com.ppip.dallyeo.course.converter.PolylineConverter;
import com.ppip.dallyeo.course.converter.WaypointAnchorsConverter;
import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.course.dto.WaypointAnchor;
import com.ppip.dallyeo.domain.region.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 코스 엔티티 (BR-U2-3, Q1=A). 경로 데이터는 JSON TEXT 컬럼(@Convert, D1).
 * 목록 성능을 위해 waypointCount는 별도 컬럼(D2=B). U2 소유, U5(사용자 코스)가 확장.
 */
@Entity
@Table(name = "course", indexes = {
        @Index(name = "idx_course_region", columnList = "region"),
        @Index(name = "idx_course_measured_category", columnList = "measuredCategory")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    private String id;

    private String name;

    /** 코스 소개 문구(courses.json description). 목록/상세 응답에 노출. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 코스 대표 이미지 URL. 정적 리소스 규약: {@code /images/courses/{id}.png}
     * (파일은 classpath {@code static/images/courses/}). 이미지 미제공 코스는 null.
     */
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Enumerated(EnumType.STRING)
    private CourseDistance declaredCategory;

    @Enumerated(EnumType.STRING)
    private CourseDistance measuredCategory;

    /** 용도 미정 — 저장만, 응답 비노출(BR-U2-8). */
    private int searchOption;

    private int totalMeters;

    /** 목록 요약용 사전 계산 컬럼(D2=B). */
    private int waypointCount;

    @Convert(converter = PolylineConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<PolylinePoint> polyline;

    @Convert(converter = IntListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<Integer> cumulativeMeters;

    @Convert(converter = WaypointAnchorsConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<WaypointAnchor> waypointAnchors;
}
