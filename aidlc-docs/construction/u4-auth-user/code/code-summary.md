# U4 인증·사용자 🔒 — Code Summary

> Code Generation Part 2 결과. 브라운필드: 신규 생성 + 기존 파일 제자리 수정. 앱 코드 = 워크스페이스 루트.
> 검증: `compileJava` / `compileTestJava` 성공, U4 순수 단위테스트 **35건 전부 통과**(0 실패/에러).

---

## 생성 파일 (Created)

### user 도메인
- `user/Provider.java` — KAKAO/APPLE enum + 경로 파싱(미지원 400)
- `user/Gender.java` — MALE/FEMALE/NONE
- `user/User.java` — JPA 엔티티, (provider, providerUserId) 유니크, onboardingCompleted 플래그, @PrePersist/@PreUpdate 타임스탬프
- `user/NicknameGenerator.java` — 소셜 닉네임 우선, 없으면 러너#### 자동생성
- `user/UserRepository.java` — findByProviderAndProviderUserId
- `user/UserService.java` — 프로필 조회/부분수정(+온보딩 완료)/하드삭제
- `user/UserController.java` — GET/PATCH/DELETE /users/me (🔒)
- `user/dto/UserProfileResponse.java`, `user/dto/UpdateProfileRequest.java`(범위/길이 검증)

### auth 도메인 + 토큰(U1-b)
- `auth/JwtProperties.java` — @ConfigurationProperties("jwt")
- `auth/JwtProvider.java` — jjwt HS256 발급/검증(sub/type/iat/exp), type 교차사용 차단, secret fail-fast
- `auth/TokenHasher.java` — SHA-256(refresh 저장용)
- `auth/RefreshTokenStore.java` — StringRedisTemplate, refresh:{userId}=해시, TTL 7d, 회전
- `auth/AuthService.java` — login(검증→upsert→토큰→refresh저장), refresh(회전), logout
- `auth/AuthController.java` — POST /auth/login/{provider}, /auth/refresh(🌐), /auth/logout(🔒)
- `auth/dto/LoginRequest.java`, `RefreshRequest.java`, `TokenResponse.java`(NON_NULL: 갱신 응답은 onboarding/user 생략)
- `auth/JwtAuthenticationFilter.java` — Bearer 검증→SecurityContext 주입
- `auth/RestAuthenticationEntryPoint.java`(401) / `auth/RestAccessDeniedHandler.java`(403) / `auth/SecurityErrorResponder.java` — 공통 ApiResponse 직렬화
- `auth/AuthUser.java`(annotation) / `auth/AuthUserArgumentResolver.java` — 현재 userId 주입

### external/oauth (소셜 검증)
- `external/oauth/OAuthUser.java`, `OAuthClient.java`(interface), `OAuthProperties.java`
- `external/oauth/KakaoOAuthClient.java` — RestClient 사용자조회 + Resilience4j(oauthKakaoUserInfo)
- `external/oauth/AppleJwksProvider.java` — JWKS fetch + 인메모리 캐시(6h) + kid 매칭 + Resilience4j(oauthAppleJwks)
- `external/oauth/AppleOAuthClient.java` — nimbus-jose-jwt RS256 검증 + iss/aud/exp
- `external/oauth/OAuthClientResolver.java` — provider→구현체(미지원 400)

### config
- `config/WebConfig.java` — AuthUserArgumentResolver 등록

### 테스트 (35건)
- `auth/JwtProviderTest`(6), `auth/RefreshTokenStoreTest`(5), `auth/AuthServiceTest`(5), `auth/AuthControllerTest`(3)
- `user/NicknameGeneratorTest`(5), `user/UserServiceTest`(6), `user/UserControllerTest`(3)
- `external/oauth/OAuthClientResolverTest`(2)

## 수정 파일 (Modified)
- `config/SecurityConfig.java` — permitAll 껍데기 → **deny-by-default + 화이트리스트** + JwtAuthenticationFilter + EntryPoint/AccessDeniedHandler + @EnableConfigurationProperties(Jwt/OAuth)
- `config/RestClientConfig.java` — kakaoRestClient/appleRestClient 빈 추가(타임아웃 2s/3s)
- `build.gradle` — jjwt-api/impl/jackson(0.12.6) + nimbus-jose-jwt(9.48) 추가
- `src/main/resources/application.properties` — jwt.access-expiration **1h→24h(86400000) 교정**, oauth.* 설정, resilience 인스턴스(oauthKakaoUserInfo/oauthAppleJwks)
- `.env.example` — APPLE_CLIENT_ID 추가

---

## 스토리 매핑
| 스토리 | 구현 |
|---|---|
| US-AUTH-1 소셜로그인 | AuthService.login + Kakao/Apple OAuthClient |
| US-AUTH-2 토큰발급/만료 | JwtProvider(24h/7d) + RefreshTokenStore |
| US-AUTH-3 토큰갱신 | AuthService.refresh(회전 + Redis 대조) |
| US-AUTH-4 인증미들웨어 | SecurityConfig(deny-by-default) + JwtAuthenticationFilter + EntryPoint/AccessDeniedHandler |
| US-AUTH-5 로그아웃 | AuthService.logout(refresh 삭제) |
| US-USER-1 온보딩 | UserService.updateProfile(+onboardingCompleted) |
| US-USER-2 프로필 | UserService.getMyProfile/updateProfile |
| US-USER-3 계정삭제 | UserService.deleteAccount(하드삭제) |

## 설계 반영
- FD: Q1=B(Kakao+Apple 실구현) · Q2=A(토큰검증, OAuthClient 추상화로 code방식 전환 여지) · Q3=A(provider+providerUserId 유니크) · Q4=A(닉네임 자동생성) · Q5=A(HS256) · Q6=A(1세션/회전) · Q7=A(하드삭제) · Q8=B(onboardingCompleted)
- NFR: Q1=A(nimbus) · Q2=A(레이트리밋 미적용) · Q3=A(refresh SHA-256 해시) · Q4=A(소셜호출 Resilience4j+JWKS캐시) · Q5=A(at-rest accepted)
- 보안: deny-by-default 화이트리스트(auth-classification §1), 토큰/credential 미로깅, 시크릿 env

## 추가 산출물 (Postman 테스트 지원, 사후 요청)
- `Dallyeo.postman_collection.json`(프로젝트 루트) — 15개 요청(Auth/Users/Regions/Courses/Places). 로그인 성공 시 access/refresh 토큰 자동 저장 스크립트 포함.
- `auth/dev/DevAuthController.java` + `DevLoginRequest.java` — **@Profile("dev") 전용** 테스트 로그인(`POST /dev/login`). 소셜 검증 없이 테스트 사용자 JWT 발급. 운영(prod)에는 빈 미생성(404). `SecurityConfig`에 `/dev/**` permitAll 추가(핸들러가 dev 전용이라 prod 노출 없음).
- 실행: `SPRING_PROFILES_ACTIVE=dev`로 부팅 시에만 `/dev/login` 활성.

## 호환성 / 유의 노트 (Build & Test 확인 대상)
- **Jackson 3 공존**: 프로젝트는 Jackson 3(`tools.jackson`). jjwt-jackson(0.12.6)은 Jackson 2(`com.fasterxml`) databind를 전이 의존으로 가져와 공존. compile/단위테스트는 정상. 실행 부팅 시 최종 확인 권장.
- **jwt.access-expiration 교정**: U1-a 선반영값 1h → US-AUTH-2에 맞춰 24h(86400000)로 변경.
- **Apple 로그인 전제**: `APPLE_CLIENT_ID`(aud) env 필요. 미설정 시 Apple 로그인만 401(Kakao는 무관).
- **부팅/통합 검증 미수행**: 이번 단계는 compile + 순수 단위테스트까지. 실 소셜/DB/Redis 연동 부팅 검증은 다음 단계(요청 시) 또는 Build & Test에서.
