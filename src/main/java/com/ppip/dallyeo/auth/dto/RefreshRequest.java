package com.ppip.dallyeo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 (api-spec §1.2).
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
