package com.ppip.dallyeo.external.oauth;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.Provider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Kakao 소셜 검증 (US-AUTH-1, BR-4.2, FD Q2=A).
 * 프론트가 전달한 Kakao access token으로 사용자 조회 API 호출 → providerUserId/nickname.
 * 회복성: Resilience4j(oauthKakaoUserInfo). 실패/무효 → UNAUTHORIZED.
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);

    private final RestClient kakaoRestClient;
    private final OAuthProperties props;

    public KakaoOAuthClient(RestClient kakaoRestClient, OAuthProperties props) {
        this.kakaoRestClient = kakaoRestClient;
        this.props = props;
    }

    @Override
    public Provider provider() {
        return Provider.KAKAO;
    }

    @Override
    @Retry(name = "oauthKakaoUserInfo", fallbackMethod = "fallback")
    @CircuitBreaker(name = "oauthKakaoUserInfo", fallbackMethod = "fallback")
    public OAuthUser verify(String accessToken) {
        JsonNode root = kakaoRestClient.get()
                .uri(props.kakao().userInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(JsonNode.class);

        if (root == null || root.path("id").isMissingNode()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 사용자 정보를 확인할 수 없습니다.");
        }
        String providerUserId = root.path("id").asText();
        JsonNode nicknameNode = root.path("kakao_account").path("profile").path("nickname");
        String nickname = nicknameNode.isMissingNode() ? null : nicknameNode.asText();
        return new OAuthUser(Provider.KAKAO, providerUserId, nickname);
    }

    @SuppressWarnings("unused")
    private OAuthUser fallback(String accessToken, Throwable t) {
        log.warn("Kakao user-info fallback: {}", t.toString());
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "카카오 인증에 실패했습니다.");
    }
}
