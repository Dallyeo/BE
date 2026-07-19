package com.ppip.dallyeo.course;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseDistanceTest {

    @Test
    void fromKorean() {
        assertThat(CourseDistance.fromKorean("단거리")).isEqualTo(CourseDistance.SHORT);
        assertThat(CourseDistance.fromKorean("중거리")).isEqualTo(CourseDistance.MEDIUM);
        assertThat(CourseDistance.fromKorean("장거리")).isEqualTo(CourseDistance.LONG);
        assertThat(CourseDistance.fromKorean("초장거리")).isNull();
        assertThat(CourseDistance.fromKorean(null)).isNull();
    }

    @Test
    void fromParam() {
        assertThat(CourseDistance.fromParam("SHORT")).isEqualTo(CourseDistance.SHORT);
        assertThat(CourseDistance.fromParam("medium")).isEqualTo(CourseDistance.MEDIUM);
        assertThat(CourseDistance.fromParam("XL")).isNull();
        assertThat(CourseDistance.fromParam("")).isNull();
        assertThat(CourseDistance.fromParam(null)).isNull();
    }
}
