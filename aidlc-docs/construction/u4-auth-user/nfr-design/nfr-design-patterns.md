# NFR Design Patterns — U4 인증·사용자 🔒 (light)

> NFR Requirements(U4) 결정을 설계 패턴으로 확정. 신규 블로킹 질문 없음(결정 Q1~Q5 확정). U1-a 회복성/캐시/로깅 패턴 상속.

---

## 1. 보안 패턴 (핵심)

### 1.1 인증 아키텍처 — Stateless JWT + deny-by-default
- **패턴**: Token-based stateless authentication. 세션 없음(`SessionCreationPolicy.STATELESS` 상속).
- **필터 체인**: `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 배치.
  - Bearer 토큰 파싱 → jjwt 검증(서명/만료/`type=access`) → `SecurityContext`에 `userId` 주입.
  - 토큰 없음/무효 → 컨텍스트 미설정 → 보호 경로는 `AuthenticationEntryPoint`가 401.
- **인가 정책**: `anyRequest().authenticated()` + 명시적 화이트리스트 `permitAll`(auth-classification §1).
- **에러 응답 정규화**: `AuthenticationEntryPoint`(401)·`AccessDeniedHandler`(403)를 공통 `ApiResponse`/`ApiError` 래퍼로 직렬화(U1-a `GlobalExceptionHandler`와 형식 일치).

### 1.2 토큰 검증/서명 패턴 (jjwt, HS256)
- `JwtProvider`: `issueAccessToken(userId)`, `issueRefreshToken(userId)`, `parseAndValidate(token, expectedType)`, `getUserId(token)`.
- 클레임: `sub=userId`, `type∈{access,refresh}`, `iat`, `exp`. type 교차사용 차단(access 자리 refresh → 거부).
- 시크릿 `JWT_SECRET`(env). 부팅 시 비어있으면 fail-fast.

### 1.3 소셜 검증 어댑터 패턴 (전략)
- `OAuthClient` 인터페이스 → `KakaoOAuthClient`, `AppleOAuthClient` 전략 구현. `OAuthClientResolver`(provider→구현체 매핑).
- **Kakao**: `RestClient`로 사용자 조회 API 호출(전달 access token) → providerUserId/nickname.
- **Apple**: `nimbus-jose-jwt`로 identity token(RS256) 검증 — JWKS에서 `kid` 매칭 공개키로 서명검증 + `iss`/`aud`/`exp` 검증 → providerUserId(`sub`).
- **확장점(Q2-FD)**: A→B(authorization code 교환) 전환 시 각 구현체 내부만 교체. 인터페이스·상위 흐름 불변.

### 1.4 Refresh Token 회전 + 해시 저장 패턴 (Q3=A, Q6-FD=A)
- `RefreshTokenStore`: `save(userId, rawToken)`(내부에서 SHA-256 해시 저장, `refresh:{userId}`, TTL 7d), `matches(userId, rawToken)`(해시 비교), `delete(userId)`.
- **회전**: 갱신 성공 시 새 refresh 해시로 덮어씀 → 이전 토큰 자동 무효(단일 세션).
- **저장 보호**: 원문 미저장(해시만) → Redis 유출 시에도 토큰 원문 비노출.

### 1.5 시크릿·로깅 보호 패턴 (SECURITY-03)
- 모든 시크릿(`JWT_SECRET`/Apple·Kakao 설정) env 주입. 코드/VCS 금지.
- `LogMaskingUtil` 확장: 토큰/Authorization 헤더/소셜 credential 마스킹. 로그인 로그는 provider·신규여부만(식별자/토큰 제외).

---

## 2. 회복성 패턴 (Q4=A, U1-a 상속)

### 2.1 소셜 외부 호출 회복성
- Kakao 사용자 조회 / Apple JWKS 호출에 Resilience4j 적용 — **엔드포인트별 인스턴스**(U1-a default 상속):
  - `oauthKakaoUserInfo`, `oauthAppleJwks` — connect 2s/read 3s + `@Retry`(최초1+재시도1) + `@CircuitBreaker`.
- **폴백 매핑**: 검증 불가/외부 오류 → 인증 실패(401). refresh/로그인 흐름은 데이터 변경 전 실패라 부작용 없음.

### 2.2 Apple JWKS 캐시
- JWKS 응답을 캐시(TTL, 예 6h). 캐시 HIT 시 외부 0콜. `kid` 미스(키 로테이션) 시 강제 리프레시 후 1회 재시도.
- 캐시 저장소: U1-a `CacheConfig`(Redis) 재사용 또는 경량 인메모리(키 소수) — Code 단계에서 확정. 캐시 다운 시 원본 fetch로 폴백(best-effort).

### 2.3 Redis 의존 실패 처리
- 로그인 시 `RefreshTokenStore.save` 실패(Redis 다운) → 500(모니터). 갱신 시 조회 실패 → 401(재로그인). 인증 흐름은 fail-safe(거부 방향).

---

## 3. 성능·확장성 패턴
- JWT 검증은 인메모리(요청당 O(1)). Redis refresh 조회는 갱신/로그아웃 시에만.
- Stateless → 앱 수평확장 자유(세션 스티키 불필요). Refresh 상태만 Redis 공유.
- 하드 성능 타깃 없음(N2 상속). 관측 로깅만.

---

## 4. 미적용/이관 (명시)
- **레이트리밋**: 미적용(Q2=A) — 후속 백로그. (남용 관측 시 `/auth/login`·`/auth/refresh`에 Bucket4j 등)
- **at-rest 암호화**: 인프라 미변경(Q5=A, accepted risk). in-transit은 nginx TLS.
- **Access Token 즉시 무효화(블랙리스트)**: 범위 밖(stateless 유지, 24h 자연만료).
- **SECURITY-04 HTTP 보안 헤더**: API-only(JSON) → N/A.

---

## 5. 화이트리스트 (설계 확정 — SecurityConfig 반영)
```
permitAll:
  POST /auth/login/*        POST /auth/refresh
  GET  /regions
  GET  /courses             GET /courses/{id}
  GET  /places/**
  GET  /actuator/health     (헬스체크)
authenticated (그 외 전부):
  POST /auth/logout
  GET/PATCH/DELETE /users/me
  (U5) POST /courses, /runs/**
```
