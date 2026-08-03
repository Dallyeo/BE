package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.auth.dto.TokenResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.external.oauth.OAuthClient;
import com.ppip.dallyeo.external.oauth.OAuthClientResolver;
import com.ppip.dallyeo.external.oauth.OAuthUser;
import com.ppip.dallyeo.user.NicknameGenerator;
import com.ppip.dallyeo.user.Provider;
import com.ppip.dallyeo.user.User;
import com.ppip.dallyeo.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final OAuthClientResolver resolver = mock(OAuthClientResolver.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);

    private final AuthService authService = new AuthService(
            resolver, userRepository, nicknameGenerator, jwtProvider, refreshTokenStore);

    private final OAuthClient kakaoClient = mock(OAuthClient.class);

    @Test
    void login_newUser_createsAndRequiresOnboarding() {
        when(resolver.resolve(Provider.KAKAO)).thenReturn(kakaoClient);
        when(kakaoClient.verify("cred")).thenReturn(new OAuthUser(Provider.KAKAO, "pid-1", "카카오닉"));
        when(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "pid-1"))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.resolve("카카오닉")).thenReturn("카카오닉");
        when(userRepository.save(any(User.class))).thenReturn(
                User.builder().id(1L).provider(Provider.KAKAO).providerUserId("pid-1")
                        .nickname("카카오닉").onboardingCompleted(false).build());
        when(jwtProvider.issueAccessToken(1L)).thenReturn("access");
        when(jwtProvider.issueRefreshToken(1L)).thenReturn("refresh");
        when(jwtProvider.accessTokenExpiresInSeconds()).thenReturn(86_400L);

        TokenResponse res = authService.login(Provider.KAKAO, "cred");

        assertThat(res.accessToken()).isEqualTo("access");
        assertThat(res.refreshToken()).isEqualTo("refresh");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.accessTokenExpiresIn()).isEqualTo(86_400L);
        assertThat(res.onboardingRequired()).isTrue();
        assertThat(res.user().id()).isEqualTo(1L);
        verify(refreshTokenStore).save(1L, "refresh");
    }

    @Test
    void login_existingOnboardedUser_noOnboarding() {
        User existing = User.builder().id(2L).provider(Provider.KAKAO).providerUserId("pid-2")
                .nickname("기존").onboardingCompleted(true).build();
        when(resolver.resolve(Provider.KAKAO)).thenReturn(kakaoClient);
        when(kakaoClient.verify("cred")).thenReturn(new OAuthUser(Provider.KAKAO, "pid-2", "기존"));
        when(userRepository.findByProviderAndProviderUserId(Provider.KAKAO, "pid-2"))
                .thenReturn(Optional.of(existing));
        when(jwtProvider.issueAccessToken(2L)).thenReturn("a");
        when(jwtProvider.issueRefreshToken(2L)).thenReturn("r");
        when(jwtProvider.accessTokenExpiresInSeconds()).thenReturn(86_400L);

        TokenResponse res = authService.login(Provider.KAKAO, "cred");

        assertThat(res.onboardingRequired()).isFalse();
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void refresh_rotatesTokens() {
        when(jwtProvider.parseUserId("old-refresh", JwtProvider.TYPE_REFRESH)).thenReturn(5L);
        when(refreshTokenStore.matches(5L, "old-refresh")).thenReturn(true);
        when(jwtProvider.issueAccessToken(5L)).thenReturn("new-access");
        when(jwtProvider.issueRefreshToken(5L)).thenReturn("new-refresh");
        when(jwtProvider.accessTokenExpiresInSeconds()).thenReturn(86_400L);

        TokenResponse res = authService.refresh("old-refresh");

        assertThat(res.accessToken()).isEqualTo("new-access");
        assertThat(res.refreshToken()).isEqualTo("new-refresh");
        assertThat(res.onboardingRequired()).isNull();
        verify(refreshTokenStore).save(5L, "new-refresh");
    }

    @Test
    void refresh_mismatch_unauthorized() {
        when(jwtProvider.parseUserId("bad", JwtProvider.TYPE_REFRESH)).thenReturn(5L);
        when(refreshTokenStore.matches(5L, "bad")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void logout_deletesRefresh() {
        authService.logout(9L);

        verify(refreshTokenStore).delete(9L);
    }
}
