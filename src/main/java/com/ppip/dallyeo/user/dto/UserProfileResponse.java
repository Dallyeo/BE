package com.ppip.dallyeo.user.dto;

import com.ppip.dallyeo.user.Gender;
import com.ppip.dallyeo.user.User;

/**
 * 프로필 응답 (api-spec §2.1). profileImageUrl은 보류(항상 null).
 */
public record UserProfileResponse(
        Long id,
        String nickname,
        Gender gender,
        Double height,
        Double weight,
        String profileImageUrl
) {
    public static UserProfileResponse from(User u) {
        return new UserProfileResponse(
                u.getId(), u.getNickname(), u.getGender(),
                u.getHeight(), u.getWeight(), u.getProfileImageUrl());
    }
}
