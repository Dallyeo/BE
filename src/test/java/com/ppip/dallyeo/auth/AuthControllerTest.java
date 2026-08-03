package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.auth.dto.LoginRequest;
import com.ppip.dallyeo.auth.dto.TokenResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.user.Provider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthController controller = new AuthController(authService);

    @Test
    void login_unsupportedProvider_400() {
        assertThatThrownBy(() -> controller.login("naver", new LoginRequest("code")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void login_delegatesToService() {
        TokenResponse token = TokenResponse.login("a", "r", 86_400L, true, null);
        when(authService.login(Provider.KAKAO, "code")).thenReturn(token);

        ApiResponse<TokenResponse> res = controller.login("kakao", new LoginRequest("code"));

        assertThat(res.success()).isTrue();
        assertThat(res.data()).isSameAs(token);
    }

    @Test
    void logout_delegatesToService() {
        controller.logout(3L);

        verify(authService).logout(3L);
    }
}
