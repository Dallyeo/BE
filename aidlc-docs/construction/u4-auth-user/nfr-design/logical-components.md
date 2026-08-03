# Logical Components — U4 인증·사용자 🔒 (NFR Design)

> NFR 패턴(`nfr-design-patterns.md`)을 뒷받침하는 논리 컴포넌트/배선. 도메인 타입은 `functional-design/domain-entities.md` 참조.
> U4 = auth/user 도메인 + **U1-b**(보안 인프라). U1-a `SecurityConfig` 껍데기를 **교체**.

---

## 1. 컴포넌트 목록

| 논리 컴포넌트 | 유형 | 역할 | NFR 근거 |
|---|---|---|---|
| `SecurityConfig` | @Configuration | deny-by-default + 화이트리스트 + 필터 등록(U1-a permitAll **교체**) | 보안 1.1 |
| `JwtAuthenticationFilter` | OncePerRequestFilter | Bearer 파싱→검증→SecurityContext 주입 | 보안 1.1 |
| `JwtProvider` | 컴포넌트 | 토큰 발급/검증(jjwt, HS256) | 보안 1.2 |
| `JwtProperties` | @ConfigurationProperties | secret/access-ttl/refresh-ttl 바인딩 | 보안/설정외부화 |
| `RestAuthEntryPoint` | AuthenticationEntryPoint | 401을 공통 `ApiResponse`로 | 신뢰성/보안 |
| `RestAccessDeniedHandler` | AccessDeniedHandler | 403을 공통 `ApiResponse`로 | 신뢰성/보안 |
| `@AuthUser` + `AuthUserArgumentResolver` | ArgumentResolver | 컨트롤러에 현재 userId 주입 | 유지보수 |
| `OAuthClient`(interface) | 전략 인터페이스 | `OAuthUser verify(credential)` | 보안 1.3 |
| `KakaoOAuthClient` | 전략 구현 | RestClient 사용자 조회 + 회복성 | 보안/가용성 |
| `AppleOAuthClient` | 전략 구현 | nimbus JWKS RS256 검증 + 회복성/캐시 | 보안/가용성 |
| `OAuthClientResolver` | 팩토리 | provider→구현체 매핑, 미지원 400 | 신뢰성 |
| `AppleJwksProvider` | 컴포넌트 | JWKS fetch·캐시·kid 매칭 | 가용성 2.2 |
| `RefreshTokenStore` | 컴포넌트 | Redis refresh 해시 저장/대조/삭제(회전) | 보안 1.4 |
| `AuthService` | 서비스 | login/refresh/logout 오케스트레이션 | — |
| `UserService` | 서비스 | 프로필 조회/수정/삭제 | — |
| `UserRepository` | JPA Repository | `users` CRUD + `findByProviderAndProviderUserId` | — |
| `LogMaskingUtil`(확장) | 공통 유틸 | 토큰/credential 마스킹 | 보안 1.5 |
| `ResilienceConfig`(확장) | application.properties | oauth 인스턴스 2종 추가 | 가용성 2.1 |

---

## 2. 컴포넌트별 상세

### 2.1 SecurityConfig (U1-a 교체)
- `csrf.disable()`, `httpBasic/formLogin.disable()`, `sessionCreationPolicy(STATELESS)` (기존 유지).
- `authorizeHttpRequests`: 화이트리스트(패턴 §5) `permitAll` + `anyRequest().authenticated()`.
- `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)`.
- `exceptionHandling`: `authenticationEntryPoint(RestAuthEntryPoint)`, `accessDeniedHandler(RestAccessDeniedHandler)`.

### 2.2 JwtAuthenticationFilter
- `OncePerRequestFilter`. Authorization 헤더 `Bearer ` 추출 → 없으면 체인 계속(미인증).
- 있으면 `JwtProvider.parseAndValidate(token, ACCESS)`; 성공 시 `UsernamePasswordAuthenticationToken(userId, null, authorities)` 구성 → `SecurityContextHolder` 설정.
- 검증 예외는 컨텍스트 미설정 상태로 통과 → EntryPoint가 401(필터에서 직접 응답 쓰지 않음, 일관성).

### 2.3 JwtProvider / JwtProperties
```
jwt.secret=${JWT_SECRET}
jwt.access-token-ttl=24h
jwt.refresh-token-ttl=7d
```
- jjwt `SecretKey`(HS256). 발급: sub/type/iat/exp. 검증: 서명·만료·type 일치. `getUserId`=sub 파싱.
- fail-fast: secret 공백 시 기동 실패.

### 2.4 OAuth 전략 (Resolver + Kakao/Apple)
- `OAuthClientResolver.resolve(provider)` → 미지원 provider `BusinessException`(400).
- **KakaoOAuthClient**: `RestClient`로 `oauth.kakao.user-info-uri` 호출(Authorization: Bearer {credential}). `@CircuitBreaker/@Retry(name=oauthKakaoUserInfo)`. 응답→`OAuthUser(KAKAO, id, nickname)`. 401/무효 → 인증 실패.
- **AppleOAuthClient**: `AppleJwksProvider`에서 공개키 획득 → nimbus로 서명·클레임 검증. `OAuthUser(APPLE, sub, null)`(닉네임 없음→자동생성 대상).
- **AppleJwksProvider**: `oauth.apple.jwks-uri` fetch, JWKSet 캐시(TTL 6h). `@CircuitBreaker/@Retry(name=oauthAppleJwks)`. `kid` 미스 시 강제 리프레시 1회.

### 2.5 RefreshTokenStore (Redis, 해시)
- `save(userId, raw)`: `SET refresh:{userId} = sha256(raw)` `EX 7d`.
- `matches(userId, raw)`: 저장 해시 == sha256(raw). 키 없음/불일치 → false → 401.
- `delete(userId)`: 로그아웃/계정삭제/회전 전 제거. 회전은 save 덮어쓰기로 처리.

### 2.6 AuthService / UserService / UserRepository
- `AuthService.login`: Resolver→verify→`UserRepository.upsert`(findByProviderAndProviderUserId or create[nickname 자동생성])→JwtProvider 2토큰→RefreshTokenStore.save→onboardingRequired=!onboardingCompleted.
- `AuthService.refresh`: JwtProvider(REFRESH)검증→store.matches→회전(새 2토큰+save)→응답.
- `AuthService.logout`: store.delete(userId)→204.
- `UserService`: getMyProfile / updateProfile(부분갱신+onboardingCompleted=true) / deleteAccount(하드삭제+store.delete).
- `UserRepository`: `(provider, providerUserId)` 유니크 제약.

### 2.7 LogMaskingUtil / ResilienceConfig 확장
- 마스킹 확장: `maskToken`, Authorization 헤더/credential 치환.
- Resilience 인스턴스 추가(default 상속):
```
resilience4j.circuitbreaker.instances.oauthKakaoUserInfo.base-config=default
resilience4j.circuitbreaker.instances.oauthAppleJwks.base-config=default
resilience4j.retry.instances.oauthKakaoUserInfo.base-config=default
resilience4j.retry.instances.oauthAppleJwks.base-config=default
```

---

## 3. 컴포넌트 상호작용 (로그인 흐름)
```
AuthController.login(provider, {credential})
  └─ OAuthClientResolver.resolve(provider)
       ├─ Kakao: RestClient 사용자조회 [@Retry→@CircuitBreaker(oauthKakaoUserInfo)]
       └─ Apple: AppleJwksProvider(JWKS 캐시) → nimbus 검증 [oauthAppleJwks]
     → OAuthUser
  └─ UserRepository.findByProviderAndProviderUserId
       ├─ 존재: 기존 User
       └─ 없음: create(nickname=소셜|자동생성, onboardingCompleted=false)
  └─ JwtProvider.issueAccess/Refresh
  └─ RefreshTokenStore.save(userId, refresh[해시])   [Redis EX 7d]
  └─ TokenResponse(+onboardingRequired,+user)
  로깅: LogMaskingUtil(토큰/credential 마스킹), provider·신규여부만
```

## 4. 산출 경계
- **U4/U1-b 실제 구현**: 위 전 컴포넌트 + SecurityConfig 교체 + oauth Resilience 인스턴스.
- **U5로 이관**: runs/courses 소유권 검증 컴포넌트(토큰 userId 활용), 계정삭제 cascade 확장.
- **후속(defer)**: 레이트리밋, at-rest 암호화, 토큰 블랙리스트.
