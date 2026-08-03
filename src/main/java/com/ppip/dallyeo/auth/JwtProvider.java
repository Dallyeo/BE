package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 발급/검증 (US-AUTH-2, BR-1). HS256 대칭키(env JWT_SECRET).
 * 클레임: sub=userId, type=access|refresh, iat, exp. type 교차사용 차단(BR-1.3).
 * secret 공백 시 부팅 실패(fail-fast, BR-1.2).
 */
@Component
public class JwtProvider {

    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(JwtProperties props) {
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException("JWT_SECRET이 설정되지 않았습니다.");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = props.accessExpiration();
        this.refreshExpirationMs = props.refreshExpiration();
    }

    public String issueAccessToken(Long userId) {
        return issue(userId, TYPE_ACCESS, accessExpirationMs);
    }

    public String issueRefreshToken(Long userId) {
        return issue(userId, TYPE_REFRESH, refreshExpirationMs);
    }

    /** Access Token 만료(초) — 응답 accessTokenExpiresIn 용. */
    public long accessTokenExpiresInSeconds() {
        return accessExpirationMs / 1000;
    }

    private String issue(Long userId, String type, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMs))
                .signWith(key)
                .compact();
    }

    /**
     * 서명·만료·type 검증 후 userId 반환. 실패 시 401(UNAUTHORIZED).
     * @param expectedType {@link #TYPE_ACCESS} 또는 {@link #TYPE_REFRESH}
     */
    public Long parseUserId(String token, String expectedType) {
        Claims claims = parse(token);
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰 유형이 올바르지 않습니다.");
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "토큰 정보가 올바르지 않습니다.");
        }
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }
}
