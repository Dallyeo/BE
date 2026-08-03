package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-0123456789-abcdefghijklmnop";
    private static final long ACCESS_MS = 86_400_000L;   // 24h
    private static final long REFRESH_MS = 604_800_000L; // 7d

    private JwtProvider provider(long accessMs) {
        return new JwtProvider(new JwtProperties(SECRET, accessMs, REFRESH_MS));
    }

    @Test
    void issueAndParseAccessToken() {
        JwtProvider provider = provider(ACCESS_MS);
        String token = provider.issueAccessToken(42L);

        assertThat(provider.parseUserId(token, JwtProvider.TYPE_ACCESS)).isEqualTo(42L);
    }

    @Test
    void accessTokenExpiresInSeconds() {
        assertThat(provider(ACCESS_MS).accessTokenExpiresInSeconds()).isEqualTo(86_400L);
    }

    @Test
    void typeMismatchRejected() {
        JwtProvider provider = provider(ACCESS_MS);
        String access = provider.issueAccessToken(1L);

        assertThatThrownBy(() -> provider.parseUserId(access, JwtProvider.TYPE_REFRESH))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void expiredTokenRejected() {
        JwtProvider expiring = provider(-1000L);   // 이미 만료
        String token = expiring.issueAccessToken(1L);

        assertThatThrownBy(() -> expiring.parseUserId(token, JwtProvider.TYPE_ACCESS))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void tamperedTokenRejected() {
        JwtProvider provider = provider(ACCESS_MS);
        String token = provider.issueAccessToken(1L) + "tampered";

        assertThatThrownBy(() -> provider.parseUserId(token, JwtProvider.TYPE_ACCESS))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void blankSecretFailsFast() {
        assertThatThrownBy(() -> new JwtProvider(new JwtProperties("  ", ACCESS_MS, REFRESH_MS)))
                .isInstanceOf(IllegalStateException.class);
    }
}
