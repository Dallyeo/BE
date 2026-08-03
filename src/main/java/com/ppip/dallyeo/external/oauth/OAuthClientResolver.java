package com.ppip.dallyeo.external.oauth;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.Provider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * provider → OAuthClient 구현체 매핑 (BR-4.1). 미지원 provider → 400.
 * 등록된 모든 OAuthClient 빈을 자동 수집.
 */
@Component
public class OAuthClientResolver {

    private final Map<Provider, OAuthClient> byProvider = new EnumMap<>(Provider.class);

    public OAuthClientResolver(List<OAuthClient> clients) {
        for (OAuthClient client : clients) {
            byProvider.put(client.provider(), client);
        }
    }

    public OAuthClient resolve(Provider provider) {
        OAuthClient client = byProvider.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 provider입니다: " + provider);
        }
        return client;
    }
}
