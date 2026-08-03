package com.ppip.dallyeo.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token 저장/대조/삭제 (US-AUTH-2/3/5, BR-2). Redis, 사용자당 1개(회전).
 * 키 refresh:{userId} = SHA-256 해시(NFR Q3=A), TTL = refresh 만료(7d).
 * 캐시 매니저(RedisCacheManager)와 분리된 StringRedisTemplate 사용.
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RefreshTokenStore(StringRedisTemplate redis, JwtProperties props) {
        this.redis = redis;
        this.ttl = Duration.ofMillis(props.refreshExpiration());
    }

    /** 저장(회전 시 기존 값 덮어씀 → 이전 토큰 무효). raw는 해시로 저장. */
    public void save(Long userId, String rawRefreshToken) {
        redis.opsForValue().set(key(userId), TokenHasher.sha256(rawRefreshToken), ttl);
    }

    /** 저장된 해시와 전달 토큰의 해시가 일치하는지(BR-2.3). 키 없음/불일치 → false. */
    public boolean matches(Long userId, String rawRefreshToken) {
        String stored = redis.opsForValue().get(key(userId));
        return stored != null && stored.equals(TokenHasher.sha256(rawRefreshToken));
    }

    /** 로그아웃/계정삭제 시 무효화(BR-2.5). */
    public void delete(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
