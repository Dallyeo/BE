package com.ppip.dallyeo.common.exception;

import com.ppip.dallyeo.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgument_isBadRequest() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleIllegalArgument(new IllegalArgumentException("bad param"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().success()).isFalse();
        assertThat(res.getBody().error().code()).isEqualTo("BAD_REQUEST");
        assertThat(res.getBody().error().message()).isEqualTo("bad param");
    }

    @Test
    void externalApi_isBadGateway() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleExternal(new ExternalApiException("tour down"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(res.getBody().error().code()).isEqualTo("EXTERNAL_API_ERROR");
    }

    @Test
    void business_usesErrorCodeStatus() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().error().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void unexpected_isInternalErrorWithoutLeakingMessage() {
        ResponseEntity<ApiResponse<Void>> res =
                handler.handleUnexpected(new RuntimeException("stacktrace secret"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        // 내부 원문 미노출 — 기본 메시지로 대체
        assertThat(res.getBody().error().message()).isEqualTo(ErrorCode.INTERNAL_ERROR.defaultMessage());
    }
}
