package com.ppip.dallyeo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 영업시간 캐시만 TTL이 길어야 한다 — 목록 채우기가 항목당 1회 호출이라
 * 공공데이터포털 일일 한도가 실질 제약이고, 영업시간은 하루 단위로 바뀌지 않는다.
 * 프로퍼티에 적는 것과 실제 캐시 설정에 반영되는 것은 별개라 여기서 확인한다.
 */
@SpringBootTest
class CacheTtlTest {

    @Autowired
    RedisCacheManager cacheManager;

    /** 실제로 캐시를 꺼내 확인한다 — 런타임에 쓰이는 바로 그 설정이다. */
    private Duration ttlOf(String cacheName) {
        RedisCache cache = (RedisCache) cacheManager.getCache(cacheName);
        return cache.getCacheConfiguration().getTtlFunction().getTimeToLive(Object.class, Object.class);
    }

    @Test
    void introCacheKeepsEntriesFarLongerThanTheDefault() {
        assertThat(ttlOf("tourApiDetailIntro")).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void otherTourApiCachesKeepTheDefaultTtl() {
        assertThat(ttlOf("tourApiAreaBased")).isEqualTo(Duration.ofMinutes(30));
        assertThat(ttlOf("tourApiDetailCommon")).isEqualTo(Duration.ofMinutes(30));
    }
}
