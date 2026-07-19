package com.ppip.dallyeo.course;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.course.dto.CourseDetail;
import com.ppip.dallyeo.course.dto.CourseSummary;
import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.course.dto.WaypointAnchor;
import com.ppip.dallyeo.domain.region.Region;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseQueryServiceTest {

    private final CourseRepository repository = mock(CourseRepository.class);
    private final CourseQueryService service = new CourseQueryService(repository);

    private Course sample() {
        return Course.builder()
                .id("gunsan-modern-history-run").name("근대 역사 박물관 런")
                .region(Region.GUNSAN)
                .declaredCategory(CourseDistance.MEDIUM).measuredCategory(CourseDistance.MEDIUM)
                .searchOption(0).totalMeters(10604).waypointCount(7)
                .polyline(List.of(new PolylinePoint(35.9, 126.6)))
                .cumulativeMeters(List.of(0, 25))
                .waypointAnchors(List.of(new WaypointAnchor("은파호수공원", 0)))
                .build();
    }

    @Test
    void findSummaries_mapsMeasuredCategoryAndWaypointCount() {
        when(repository.findByFilters(Region.GUNSAN, null)).thenReturn(List.of(sample()));

        List<CourseSummary> result = service.findSummaries(Region.GUNSAN, null);

        assertThat(result).hasSize(1);
        CourseSummary s = result.get(0);
        assertThat(s.distanceCategory()).isEqualTo(CourseDistance.MEDIUM);
        assertThat(s.waypointCount()).isEqualTo(7);
        assertThat(s.totalMeters()).isEqualTo(10604);
    }

    @Test
    void findDetail_returnsFullCourse() {
        when(repository.findById("gunsan-modern-history-run")).thenReturn(Optional.of(sample()));

        CourseDetail d = service.findDetail("gunsan-modern-history-run");

        assertThat(d.polyline()).hasSize(1);
        assertThat(d.cumulativeMeters()).containsExactly(0, 25);
        assertThat(d.waypointAnchors()).hasSize(1);
        assertThat(d.distanceCategory()).isEqualTo(CourseDistance.MEDIUM);
    }

    @Test
    void findDetail_notFound_throws404() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail("nope"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
