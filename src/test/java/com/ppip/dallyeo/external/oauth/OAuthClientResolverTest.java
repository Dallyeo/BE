package com.ppip.dallyeo.external.oauth;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.Provider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthClientResolverTest {

    private final OAuthClient kakao = mock(OAuthClient.class);

    @Test
    void resolvesRegisteredProvider() {
        when(kakao.provider()).thenReturn(Provider.KAKAO);
        OAuthClientResolver resolver = new OAuthClientResolver(List.of(kakao));

        assertThat(resolver.resolve(Provider.KAKAO)).isSameAs(kakao);
    }

    @Test
    void unregisteredProvider_badRequest() {
        when(kakao.provider()).thenReturn(Provider.KAKAO);
        OAuthClientResolver resolver = new OAuthClientResolver(List.of(kakao));

        assertThatThrownBy(() -> resolver.resolve(Provider.APPLE))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
