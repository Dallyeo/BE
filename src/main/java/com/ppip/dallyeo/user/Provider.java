package com.ppip.dallyeo.user;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;

/**
 * 소셜 로그인 제공자 (FD Q1=B: Kakao+Apple 실구현). User 소유 속성.
 */
public enum Provider {
    KAKAO,
    APPLE;

    /** 경로 변수(kakao/apple) → enum. 미지원 → 400 (BR-4.1). */
    public static Provider from(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "provider는 필수입니다.");
        }
        try {
            return Provider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 provider입니다: " + value);
        }
    }
}
