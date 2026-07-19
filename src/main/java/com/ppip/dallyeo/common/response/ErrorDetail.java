package com.ppip.dallyeo.common.response;

/**
 * 필드 단위 오류 상세 (BR-2). 주로 Bean Validation 실패 시 채워진다.
 */
public record ErrorDetail(String field, String message) {
}
