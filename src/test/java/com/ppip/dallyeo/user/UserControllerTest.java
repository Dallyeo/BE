package com.ppip.dallyeo.user;

import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.user.dto.UpdateProfileRequest;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private final UserService userService = mock(UserService.class);
    private final UserController controller = new UserController(userService);

    private final UserProfileResponse profile =
            new UserProfileResponse(1L, "닉", Gender.NONE, null, null, null);

    @Test
    void getMyProfile_delegates() {
        when(userService.getMyProfile(1L)).thenReturn(profile);

        ApiResponse<UserProfileResponse> res = controller.getMyProfile(1L);

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isSameAs(profile);
    }

    @Test
    void updateMyProfile_delegates() {
        UpdateProfileRequest req = new UpdateProfileRequest("새닉", null, null, null);
        when(userService.updateProfile(1L, req)).thenReturn(profile);

        ApiResponse<UserProfileResponse> res = controller.updateMyProfile(1L, req);

        assertThat(res.data()).isSameAs(profile);
    }

    @Test
    void deleteMyAccount_delegates() {
        controller.deleteMyAccount(1L);

        verify(userService).deleteAccount(1L);
    }
}
