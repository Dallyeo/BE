package com.ppip.dallyeo.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * 프로필/온보딩 수정 요청 (US-USER-1/2, BR-6.3/6.4). 부분 갱신: 전달된(non-null) 필드만 반영.
 * 온보딩 건너뛰기 = 빈 바디 PATCH. gender는 서비스에서 파싱(잘못된 값 → 400).
 * 검증은 non-null일 때만 적용된다.
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 20, message = "nickname은 1~20자여야 합니다.")
        String nickname,

        String gender,

        @DecimalMin(value = "50.0", message = "height는 50~250(cm) 범위여야 합니다.")
        @DecimalMax(value = "250.0", message = "height는 50~250(cm) 범위여야 합니다.")
        Double height,

        @DecimalMin(value = "20.0", message = "weight는 20~300(kg) 범위여야 합니다.")
        @DecimalMax(value = "300.0", message = "weight는 20~300(kg) 범위여야 합니다.")
        Double weight
) {
}
