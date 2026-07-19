package com.ppip.dallyeo.common.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_wrapsData() {
        ApiResponse<String> res = ApiResponse.success("hello");

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isEqualTo("hello");
        assertThat(res.error()).isNull();
    }

    @Test
    void success_withoutData() {
        ApiResponse<Void> res = ApiResponse.emptySuccess();

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isNull();
        assertThat(res.error()).isNull();
    }

    @Test
    void failure_carriesError() {
        ApiError error = ApiError.of("VALIDATION_ERROR", "invalid",
                List.of(new ErrorDetail("name", "must not be blank")));

        ApiResponse<Void> res = ApiResponse.failure(error);

        assertThat(res.success()).isFalse();
        assertThat(res.data()).isNull();
        assertThat(res.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(res.error().details()).hasSize(1);
    }

    @Test
    void apiError_dropsEmptyDetails() {
        ApiError error = ApiError.of("BAD_REQUEST", "bad", List.of());
        assertThat(error.details()).isNull();
    }
}
