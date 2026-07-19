package com.ppip.dallyeo.badge;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 실제 classpath CSV(모범음식점 UTF-8 52 / 착한가격 EUC-KR 60→요식업 45)로 적재 로직 검증. DB는 mock.
 */
class BadgeCsvLoaderTest {

    private final BadgeRepository repository = mock(BadgeRepository.class);
    private final BadgeCsvLoader loader = new BadgeCsvLoader(repository, new AddressNormalizer());

    @Test
    void loadsModelAndFoodOnlyGoodPrice() {
        when(repository.existsByTypeAndNormalizedNameAndNormalizedAddress(any(), anyString(), anyString()))
                .thenReturn(false);

        loader.run(null);

        // 모범음식점 52 + 착한가격 요식업 45(비요식 15 제외) = 97
        verify(repository, times(97)).save(any(Badge.class));
        verify(repository, times(52)).save(argThatType(BadgeType.MODEL_RESTAURANT));
        verify(repository, times(45)).save(argThatType(BadgeType.GOOD_PRICE));
    }

    @Test
    void idempotent_skipsExisting() {
        when(repository.existsByTypeAndNormalizedNameAndNormalizedAddress(any(), anyString(), anyString()))
                .thenReturn(true);

        loader.run(null);

        verify(repository, never()).save(any(Badge.class));
    }

    private Badge argThatType(BadgeType type) {
        return org.mockito.ArgumentMatchers.argThat(b -> b != null && b.getType() == type);
    }
}
