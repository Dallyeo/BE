package com.ppip.dallyeo.course;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 실제 classpath data/courses.json(10코스)로 시드 로직 검증. DB는 mock.
 */
class CourseDataLoaderTest {

    private final CourseRepository repository = mock(CourseRepository.class);
    private final CourseDataLoader loader = new CourseDataLoader(repository, new ObjectMapper());

    @Test
    void insertsAllWhenEmpty() {
        when(repository.existsById(anyString())).thenReturn(false);

        loader.run(null);

        verify(repository, times(10)).save(any(Course.class)); // 10개 전부 매핑·삽입
    }

    @Test
    void idempotent_skipsExisting() {
        when(repository.existsById(anyString())).thenReturn(true);

        loader.run(null);

        verify(repository, never()).save(any(Course.class)); // 재기동 시 중복 없음(Q4=B)
    }
}
