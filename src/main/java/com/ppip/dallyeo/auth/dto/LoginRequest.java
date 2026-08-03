package com.ppip.dallyeo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청 (api-spec §1.1). authorizationCode = 소셜 access token 또는 identity token
 * (FD Q2=A). 필드명은 중립적 — 훗날 authorization code 방식 전환에도 계약 유지.
 */
public record LoginRequest(
        @NotBlank(message = "authorizationCode는 필수입니다.")
        String authorizationCode
) {
}
