package com.ppip.dallyeo.config;

import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 캐시 설정 (D3: @Cacheable 추상화, BR-6: TTL 30분+).
 * 키=String, 값=JSON(타입정보 포함). null 값 캐싱 비활성(실패/빈 예외 캐시 방지).
 */
@Configuration
// 캐시 어드바이저를 최우선(outermost)으로 → 캐시 HIT 시 Resilience4j/외부호출을 건너뛴다(D4).
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

    @Bean
    public RedisCacheManager tourApiCacheManager(RedisConnectionFactory connectionFactory,
                                                 TourApiProperties props) {
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(props.cacheTtl())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(GenericJacksonJsonRedisSerializer.builder().build()));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
