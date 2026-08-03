package com.ppip.dallyeo.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenStoreTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final JwtProperties props = new JwtProperties("secret", 86_400_000L, 604_800_000L);
    private final RefreshTokenStore store = new RefreshTokenStore(redis, props);

    @Test
    void saveStoresHashWithTtl() {
        when(redis.opsForValue()).thenReturn(ops);

        store.save(7L, "raw-refresh");

        verify(ops).set(eq("refresh:7"),
                eq(TokenHasher.sha256("raw-refresh")),
                eq(Duration.ofMillis(604_800_000L)));
    }

    @Test
    void matchesWhenHashEquals() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("refresh:7")).thenReturn(TokenHasher.sha256("raw-refresh"));

        assertThat(store.matches(7L, "raw-refresh")).isTrue();
    }

    @Test
    void doesNotMatchDifferentToken() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("refresh:7")).thenReturn(TokenHasher.sha256("other"));

        assertThat(store.matches(7L, "raw-refresh")).isFalse();
    }

    @Test
    void doesNotMatchWhenMissing() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("refresh:7")).thenReturn(null);

        assertThat(store.matches(7L, "raw-refresh")).isFalse();
    }

    @Test
    void deleteRemovesKey() {
        store.delete(7L);

        verify(redis).delete("refresh:7");
    }
}
