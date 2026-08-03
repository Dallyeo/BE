package com.ppip.dallyeo.user;

import com.ppip.dallyeo.auth.RefreshTokenStore;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.dto.UpdateProfileRequest;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필/계정 (US-USER-1/2/3, business-logic-model §5~7).
 * 프로필 조회/수정(부분 갱신 + 온보딩 완료), 계정 하드 삭제.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenStore refreshTokenStore;

    public UserService(UserRepository userRepository, RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
    }

    /** 내 프로필 조회 (US-USER-2). */
    public UserProfileResponse getMyProfile(Long userId) {
        return UserProfileResponse.from(loadUser(userId));
    }

    /** 프로필/온보딩 수정 — 부분 갱신 + onboardingCompleted=true (US-USER-1/2, BR-6.2/6.3). */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = loadUser(userId);
        if (req.nickname() != null) {
            user.setNickname(req.nickname().trim());
        }
        if (req.gender() != null) {
            user.setGender(parseGender(req.gender()));
        }
        if (req.height() != null) {
            user.setHeight(req.height());
        }
        if (req.weight() != null) {
            user.setWeight(req.weight());
        }
        user.setOnboardingCompleted(true);   // 저장/건너뛰기 공통 = 온보딩 종료
        return UserProfileResponse.from(user);
    }

    /** 계정 삭제 — 하드 삭제 + refresh 무효화 (US-USER-3, BR-7.1). */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = loadUser(userId);
        userRepository.delete(user);
        refreshTokenStore.delete(userId);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Gender parseGender(String value) {
        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 gender입니다: " + value);
        }
    }
}
