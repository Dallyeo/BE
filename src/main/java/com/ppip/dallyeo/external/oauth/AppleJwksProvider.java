package com.ppip.dallyeo.external.oauth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.text.ParseException;
import java.time.Duration;

/**
 * Apple JWKS(공개키) 제공 (BR-4.3, NFR 회복성 2.2). 인메모리 캐시(TTL 6h).
 * kid 미스(키 로테이션) 시 강제 리프레시 1회. 회복성: Resilience4j(oauthAppleJwks).
 */
@Component
public class AppleJwksProvider {

    private static final Logger log = LoggerFactory.getLogger(AppleJwksProvider.class);
    private static final long TTL_MS = Duration.ofHours(6).toMillis();

    private final RestClient appleRestClient;
    private final OAuthProperties props;

    private volatile JWKSet cached;
    private volatile long expiresAt;

    public AppleJwksProvider(RestClient appleRestClient, OAuthProperties props) {
        this.appleRestClient = appleRestClient;
        this.props = props;
    }

    /** kid에 해당하는 공개키. 캐시 미스 시 1회 강제 리프레시. */
    @Retry(name = "oauthAppleJwks", fallbackMethod = "fallback")
    @CircuitBreaker(name = "oauthAppleJwks", fallbackMethod = "fallback")
    public JWK getKey(String kid) {
        JWKSet set = current();
        JWK jwk = set.getKeyByKeyId(kid);
        if (jwk == null) {
            jwk = refresh().getKeyByKeyId(kid);
        }
        if (jwk == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Apple 공개키를 찾을 수 없습니다.");
        }
        return jwk;
    }

    private JWKSet current() {
        JWKSet local = cached;
        if (local == null || System.currentTimeMillis() > expiresAt) {
            return refresh();
        }
        return local;
    }

    private synchronized JWKSet refresh() {
        String body = appleRestClient.get()
                .uri(props.apple().jwksUri())
                .retrieve()
                .body(String.class);
        try {
            JWKSet set = JWKSet.parse(body);
            this.cached = set;
            this.expiresAt = System.currentTimeMillis() + TTL_MS;
            return set;
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Apple 공개키 파싱에 실패했습니다.");
        }
    }

    @SuppressWarnings("unused")
    private JWK fallback(String kid, Throwable t) {
        // 외부 JWKS 조회 실패: 캐시가 있으면 캐시로 시도, 없으면 인증 실패.
        log.warn("Apple JWKS fallback (kid={}): {}", kid, t.toString());
        JWKSet local = cached;
        if (local != null) {
            JWK jwk = local.getKeyByKeyId(kid);
            if (jwk != null) {
                return jwk;
            }
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Apple 인증에 실패했습니다.");
    }
}
