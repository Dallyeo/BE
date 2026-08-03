package com.ppip.dallyeo.user;

import com.ppip.dallyeo.auth.RefreshTokenStore;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.dto.UpdateProfileRequest;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
    private final UserService userService = new UserService(userRepository, refreshTokenStore);

    private User user(long id) {
        return User.builder().id(id).provider(Provider.KAKAO).providerUserId("pid")
                .nickname("닉").gender(Gender.NONE).onboardingCompleted(false).build();
    }

    @Test
    void getMyProfile_returnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        UserProfileResponse res = userService.getMyProfile(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.nickname()).isEqualTo("닉");
        assertThat(res.profileImageUrl()).isNull();
    }

    @Test
    void getMyProfile_missing_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMyProfile(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void updateProfile_partialUpdateAndCompletesOnboarding() {
        User u = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UserProfileResponse res = userService.updateProfile(1L,
                new UpdateProfileRequest(null, "MALE", 180.0, null));

        assertThat(res.gender()).isEqualTo(Gender.MALE);
        assertThat(res.height()).isEqualTo(180.0);
        assertThat(res.weight()).isNull();
        assertThat(res.nickname()).isEqualTo("닉");   // 미전달 → 유지
        assertThat(u.isOnboardingCompleted()).isTrue();
    }

    @Test
    void updateProfile_invalidGender_badRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

        assertThatThrownBy(() -> userService.updateProfile(1L,
                new UpdateProfileRequest(null, "UNKNOWN", null, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void deleteAccount_hardDeletesAndClearsRefresh() {
        User u = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        userService.deleteAccount(1L);

        verify(userRepository).delete(u);
        verify(refreshTokenStore).delete(1L);
    }

    @Test
    void deleteAccount_missing_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteAccount(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
