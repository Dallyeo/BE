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
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/**
 * Redis 캐시 설정 (D3: @Cacheable 추상화, BR-6: TTL 30분+).
 * 키=String, 값=JSON(타입정보 포함). null 값 캐싱 비활성(실패/빈 예외 캐시 방지).
 *
 * <p><b>기본 타입 활성화 필수</b>: 활성화하지 않으면 캐시 HIT 시 DTO(record)가 LinkedHashMap으로
 * 역직렬화돼 ClassCastException이 발생한다. 검증기는 우리 패키지/표준 컬렉션으로 범위를 제한한다.
 */
@Configuration
// 캐시 어드바이저를 최우선(outermost)으로 → 캐시 HIT 시 Resilience4j/외부호출을 건너뛴다(D4).
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {

    @Bean
    public RedisCacheManager tourApiCacheManager(RedisConnectionFactory connectionFactory,
                                                 TourApiProperties props) {
        // 역직렬화 대상 타입을 우리 앱/표준 컬렉션으로 한정(임의 @class 역직렬화 차단).
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.ppip.dallyeo.")
                .allowIfSubType("java.util.")
                .build();
        GenericJacksonJsonRedisSerializer valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)   // @class 기록 → 정확한 타입으로 복원
                .build();

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(props.cacheTtl())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(valueSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }
}
