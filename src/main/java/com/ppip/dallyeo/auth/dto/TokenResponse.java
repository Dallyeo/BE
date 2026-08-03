package com.ppip.dallyeo.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ppip.dallyeo.user.dto.UserProfileResponse;

/**
 * 토큰 응답 (api-spec §1.1/1.2).
 * 로그인(§1.1)은 onboardingRequired + user 포함, 갱신(§1.2)은 두 필드 null(미포함).
 * NON_NULL 직렬화로 갱신 응답에선 두 필드가 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        Boolean onboardingRequired,
        UserProfileResponse user
) {
    private static final String BEARER = "Bearer";

    /** 로그인 응답(온보딩 여부 + 프로필 포함). */
    public static TokenResponse login(String accessToken, String refreshToken, long expiresIn,
                                      boolean onboardingRequired, UserProfileResponse user) {
        return new TokenResponse(accessToken, refreshToken, BEARER, expiresIn, onboardingRequired, user);
    }

    /** 갱신 응답(토큰만). */
    public static TokenResponse refresh(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, BEARER, expiresIn, null, null);
    }
}
