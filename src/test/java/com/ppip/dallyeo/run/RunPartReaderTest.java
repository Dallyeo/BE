package com.ppip.dallyeo.run;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * multipart 의 run 파트 파싱/검증. 요점은 <b>파트 Content-Type 에 의존하지 않는 것</b> —
 * 여기서는 문자열만 받으므로 클라이언트가 Content-Type 을 생략해도 동일하게 동작한다.
 */
class RunPartReaderTest {

    private final RunPartReader reader =
            new RunPartReader(new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator());

    private static final String VALID = """
            {"start":{"lat":35.95,"lng":126.68},"end":{"lat":35.96,"lng":126.69},
             "distanceMeters":10480,"durationSeconds":3600}
            """;

    @Test
    void readsValidJson() {
        RunCreateRequest r = reader.read(VALID);
        assertThat(r.distanceMeters()).isEqualTo(10480);
        assertThat(r.start().lat()).isEqualTo(35.95);
    }

    @Test
    void missingRequiredField_isValidationErrorNotServerError() {
        assertThatThrownBy(() -> reader.read("{\"distanceMeters\":10480}"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void malformedJson_isValidationErrorNotServerError() {
        assertThatThrownBy(() -> reader.read("{not json"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(e.getMessage()).contains("JSON 형식");
                });
    }

    @Test
    void blankPart_isValidationError() {
        assertThatThrownBy(() -> reader.read("  "))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("run 파트가 비어"));
    }

    @Test
    void nonPositiveDistance_reportsWhichFieldFailed() {
        String json = VALID.replace("\"distanceMeters\":10480", "\"distanceMeters\":0");
        assertThatThrownBy(() -> reader.read(json))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("distanceMeters"));
    }
}
