package com.ppip.dallyeo.auth.dev;

import com.ppip.dallyeo.auth.JwtProvider;
import com.ppip.dallyeo.auth.RefreshTokenStore;
import com.ppip.dallyeo.auth.dto.TokenResponse;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.user.Gender;
import com.ppip.dallyeo.user.NicknameGenerator;
import com.ppip.dallyeo.user.Provider;
import com.ppip.dallyeo.user.User;
import com.ppip.dallyeo.user.UserRepository;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ 개발/테스트 전용 로그인 (Postman 등). {@code @Profile("dev")} — 운영(prod)에서는 빈 미생성.
 * 소셜(Kakao/Apple) 검증을 건너뛰고 테스트 사용자로 토큰을 발급한다.
 * 실제 인증 흐름은 {@code /auth/login/{provider}}를 사용할 것.
 */
@Profile("dev")
@RestController
@RequestMapping("/dev")
public class DevAuthController {

    private static final String DEFAULT_TEST_USER = "tester-1";

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    public DevAuthController(UserRepository userRepository, NicknameGenerator nicknameGenerator,
                            JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.nicknameGenerator = nicknameGenerator;
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
    }

    /** 테스트 사용자로 로그인/가입 후 실제 JWT 발급(형식은 /auth/login과 동일). */
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody(required = false) DevLoginRequest request) {
        String providerUserId = resolveId(request);
        User user = userRepository
                .findByProviderAndProviderUserId(Provider.KAKAO, providerUserId)
                .orElseGet(() -> createTestUser(providerUserId));

        String accessToken = jwtProvider.issueAccessToken(user.getId());
        String refreshToken = jwtProvider.issueRefreshToken(user.getId());
        refreshTokenStore.save(user.getId(), refreshToken);

        return ApiResponse.success(TokenResponse.login(accessToken, refreshToken,
                jwtProvider.accessTokenExpiresInSeconds(),
                !user.isOnboardingCompleted(), UserProfileResponse.from(user)));
    }

    private String resolveId(DevLoginRequest request) {
        if (request != null && request.providerUserId() != null && !request.providerUserId().isBlank()) {
            return request.providerUserId().trim();
        }
        return DEFAULT_TEST_USER;
    }

    private User createTestUser(String providerUserId) {
        User user = User.builder()
                .provider(Provider.KAKAO)
                .providerUserId(providerUserId)
                .nickname(nicknameGenerator.generate())
                .gender(Gender.NONE)
                .onboardingCompleted(false)
                .build();
        return userRepository.save(user);
    }
}
