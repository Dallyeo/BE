package com.ppip.dallyeo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 실패 응답 본문 (BR-1). code는 프론트 분기용 고정 문자열 도메인 코드,
 * message는 사람이 읽는 설명, details는 선택(검증 실패 시 필드별 목록).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, List<ErrorDetail> details) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, null);
    }

    public static ApiError of(String code, String message, List<ErrorDetail> details) {
        return new ApiError(code, message, (details == null || details.isEmpty()) ? null : details);
    }
}
