package com.ppip.dallyeo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 응답 래퍼 (US-COMMON-1, business-logic-model §1).
 * 성공: {@code success=true, data=...}, 실패: {@code success=false, error=...}.
 * 본문 없는 성공(204)은 컨트롤러에서 ResponseEntity로 처리하며 이 래퍼를 쓰지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** 데이터 없는 성공(주로 204는 본문 없이 처리하나, 래퍼가 필요한 경우 사용). */
    public static <T> ApiResponse<T> emptySuccess() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
