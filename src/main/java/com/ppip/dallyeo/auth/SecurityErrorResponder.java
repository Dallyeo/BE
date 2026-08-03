package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiError;
import com.ppip.dallyeo.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증/인가 예외를 공통 {@link ApiResponse} 형식으로 직렬화 (US-AUTH-4, nfr-design 1.1).
 * EntryPoint(401)/AccessDeniedHandler(403)가 공유한다 → GlobalExceptionHandler와 응답 형식 일치.
 */
@Component
public class SecurityErrorResponder {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Void> body = ApiResponse.failure(
                ApiError.of(code.code(), code.defaultMessage(), null));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
