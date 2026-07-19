package com.ppip.dallyeo.course.converter;

import com.ppip.dallyeo.course.dto.PolylinePoint;
import com.ppip.dallyeo.course.dto.WaypointAnchor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseConvertersTest {

    @Test
    void polylineRoundTrip() {
        PolylineConverter c = new PolylineConverter();
        List<PolylinePoint> pts = List.of(new PolylinePoint(35.9, 126.6), new PolylinePoint(35.8, 126.7));

        String json = c.convertToDatabaseColumn(pts);
        assertThat(json).contains("35.9").contains("126.7");
        assertThat(c.convertToEntityAttribute(json)).isEqualTo(pts);
    }

    @Test
    void intListRoundTrip() {
        IntListConverter c = new IntListConverter();
        List<Integer> list = List.of(0, 25, 58, 104);

        assertThat(c.convertToEntityAttribute(c.convertToDatabaseColumn(list))).isEqualTo(list);
    }

    @Test
    void waypointsRoundTrip() {
        WaypointAnchorsConverter c = new WaypointAnchorsConverter();
        List<WaypointAnchor> ws = List.of(new WaypointAnchor("은파호수공원", 0), new WaypointAnchor("종점", 390));

        assertThat(c.convertToEntityAttribute(c.convertToDatabaseColumn(ws))).isEqualTo(ws);
    }

    @Test
    void nullAndBlankHandling() {
        PolylineConverter c = new PolylineConverter();
        assertThat(c.convertToDatabaseColumn(null)).isNull();
        assertThat(c.convertToEntityAttribute(null)).isEmpty();
        assertThat(c.convertToEntityAttribute("")).isEmpty();
    }
}
