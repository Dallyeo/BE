package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인가 거부(권한 없음) → 403 공통 응답 (US-AUTH-4, BR-3.4).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponder responder;

    public RestAccessDeniedHandler(SecurityErrorResponder responder) {
        this.responder = responder;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        responder.write(response, ErrorCode.FORBIDDEN);
    }
}
