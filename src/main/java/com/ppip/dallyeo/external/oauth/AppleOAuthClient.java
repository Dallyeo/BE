package com.ppip.dallyeo.external.oauth;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.user.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Apple 소셜 검증 (US-AUTH-1, BR-4.3, NFR Q1=A: nimbus-jose-jwt).
 * identity token(RS256 JWT)을 Apple JWKS 공개키로 서명검증 + iss/aud/exp 검증.
 * 검증 실패 → UNAUTHORIZED. Apple은 닉네임 미제공 → OAuthUser.nickname = null(자동생성 대상).
 */
@Component
public class AppleOAuthClient implements OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(AppleOAuthClient.class);

    private final AppleJwksProvider jwksProvider;
    private final OAuthProperties props;

    public AppleOAuthClient(AppleJwksProvider jwksProvider, OAuthProperties props) {
        this.jwksProvider = jwksProvider;
        this.props = props;
    }

    @Override
    public Provider provider() {
        return Provider.APPLE;
    }

    @Override
    public OAuthUser verify(String identityToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(identityToken);
            JWK jwk = jwksProvider.getKey(jwt.getHeader().getKeyID());
            RSAKey rsaKey = jwk.toRSAKey();
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!jwt.verify(verifier)) {
                throw unauthorized("Apple 토큰 서명 검증에 실패했습니다.");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            validateClaims(claims);
            return new OAuthUser(Provider.APPLE, claims.getSubject(), null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Apple identity token 검증 실패: {}", e.toString());
            throw unauthorized("Apple 인증에 실패했습니다.");
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        if (!props.apple().issuer().equals(claims.getIssuer())) {
            throw unauthorized("Apple 토큰 발급자가 올바르지 않습니다.");
        }
        String audience = props.apple().audience();
        if (audience == null || audience.isBlank()
                || claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw unauthorized("Apple 토큰 대상(aud)이 올바르지 않습니다.");
        }
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            throw unauthorized("Apple 토큰이 만료되었습니다.");
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw unauthorized("Apple 토큰에 사용자 식별자가 없습니다.");
        }
    }

    private BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }
}
