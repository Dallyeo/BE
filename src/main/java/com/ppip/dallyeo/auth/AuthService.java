package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.auth.dto.TokenResponse;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.external.oauth.OAuthClient;
import com.ppip.dallyeo.external.oauth.OAuthClientResolver;
import com.ppip.dallyeo.external.oauth.OAuthUser;
import com.ppip.dallyeo.user.Gender;
import com.ppip.dallyeo.user.NicknameGenerator;
import com.ppip.dallyeo.user.Provider;
import com.ppip.dallyeo.user.User;
import com.ppip.dallyeo.user.UserRepository;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;

/**
 * 인증 오케스트레이션 (US-AUTH-1/2/3/5, business-logic-model §1~3).
 * 로그인/가입 → 토큰발급 → refresh 저장. 갱신은 회전(rotation). 로그아웃은 refresh 무효화.
 */
@Service
public class AuthService {

    private final OAuthClientResolver oauthResolver;
    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(OAuthClientResolver oauthResolver, UserRepository userRepository,
                       NicknameGenerator nicknameGenerator, JwtProvider jwtProvider,
                       RefreshTokenStore refreshTokenStore) {
        this.oauthResolver = oauthResolver;
        this.userRepository = userRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    /** 소셜 로그인/가입 후 토큰 발급 (US-AUTH-1/2). */
    public TokenResponse login(Provider provider, String credential) {
        OAuthClient client = oauthResolver.resolve(provider);
        OAuthUser oauthUser = client.verify(credential);
        User user = userRepository
                .findByProviderAndProviderUserId(provider, oauthUser.providerUserId())
                .orElseGet(() -> createUser(provider, oauthUser));

        String accessToken = jwtProvider.issueAccessToken(user.getId());
        String refreshToken = jwtProvider.issueRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken);

        boolean onboardingRequired = !user.isOnboardingCompleted();
        return TokenResponse.login(accessToken, refreshToken,
                jwtProvider.accessTokenExpiresInSeconds(), onboardingRequired,
                UserProfileResponse.from(user));
    }

    private User createUser(Provider provider, OAuthUser oauthUser) {
        User user = User.builder()
                .provider(provider)
                .providerUserId(oauthUser.providerUserId())
                .nickname(nicknameGenerator.resolve(oauthUser.nickname()))
                .gender(Gender.NONE)
                .onboardingCompleted(false)
                .build();
        return userRepository.save(user);
    }

    /** 토큰 갱신 — refresh 검증 + Redis 대조 + 회전 (US-AUTH-3, BR-2). */
    public TokenResponse refresh(String refreshToken) {
        Long userId = jwtProvider.parseUserId(refreshToken, JwtProvider.TYPE_REFRESH);
        if (!refreshTokenStore.matches(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "만료되었거나 유효하지 않은 refresh 토큰입니다.");
        }
        String newAccess = jwtProvider.issueAccessToken(userId);
        String newRefresh = jwtProvider.issueRefreshToken(userId);
        refreshTokenStore.save(userId, newRefresh);   // 회전: 이전 토큰 무효
        return TokenResponse.refresh(newAccess, newRefresh, jwtProvider.accessTokenExpiresInSeconds());
    }

    /** 로그아웃 — 서버측 refresh 무효화 (US-AUTH-5). */
    public void logout(Long userId) {
        refreshTokenStore.delete(userId);
    }
}
