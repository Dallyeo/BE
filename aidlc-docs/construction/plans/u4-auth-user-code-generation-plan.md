# U4 인증·사용자 🔒 — Code Generation Plan (SSOT)

> 이 플랜이 U4 코드 생성의 **단일 진실 원천**. 승인 후 Part 2에서 스텝 순서대로 실행하며 각 스텝 [x] 갱신.
> 브라운필드: 기존 파일은 **제자리 수정**(복사본 금지). 애플리케이션 코드 = 워크스페이스 루트, 문서 = aidlc-docs.

## 유닛 컨텍스트
- **스토리**: US-AUTH-1(소셜로그인) · US-AUTH-2(토큰발급/만료) · US-AUTH-3(갱신) · US-AUTH-4(인증미들웨어) · US-AUTH-5(로그아웃) · US-USER-1(온보딩) · US-USER-2(프로필) · US-USER-3(계정삭제)
- **의존**: U1-a(ApiResponse/BusinessException/ErrorCode/GlobalExceptionHandler/RestClient/Resilience4j/LogMaskingUtil) + U1-b(이번에 함께 구현: JWT/필터/OAuth)
- **엔티티 소유**: `users` 테이블(MySQL) + Redis `refresh:{userId}`
- **결정 반영**: FD Q1=B(Kakao+Apple) Q2=A Q3=A Q4=A Q5=A(HS256) Q6=A(1세션/회전) Q7=A(하드삭제) Q8=B(onboardingCompleted) / NFR Q1=A(nimbus) Q2=A(레이트리밋X) Q3=A(refresh SHA-256 해시) Q4=A(Resilience4j) Q5=A(at-rest accepted)

## 코드 조직 (신규/수정 파일)
```
com.ppip.dallyeo
├─ user/                         # 사용자 도메인 (신규)
│  ├─ User.java (엔티티)  Provider.java  Gender.java
│  ├─ UserRepository.java  UserService.java  UserController.java
│  ├─ NicknameGenerator.java
│  └─ dto/ UserProfileResponse.java  UpdateProfileRequest.java
├─ auth/                         # 인증 도메인 + 토큰 (신규)
│  ├─ AuthController.java  AuthService.java
│  ├─ JwtProvider.java  JwtProperties.java  JwtAuthenticationFilter.java
│  ├─ RefreshTokenStore.java  TokenHasher.java
│  ├─ RestAuthenticationEntryPoint.java  RestAccessDeniedHandler.java
│  ├─ AuthUser.java(annotation)  AuthUserArgumentResolver.java
│  └─ dto/ LoginRequest.java  RefreshRequest.java  TokenResponse.java
├─ external/oauth/               # 소셜 검증 (신규)
│  ├─ OAuthClient.java(interface)  OAuthUser.java  OAuthClientResolver.java
│  ├─ KakaoOAuthClient.java  AppleOAuthClient.java  AppleJwksProvider.java
│  ├─ OAuthProperties.java
│  └─ dto/ KakaoUserResponse.java
└─ config/
   ├─ SecurityConfig.java        # 수정(permitAll → deny-by-default)
   ├─ RestClientConfig.java      # 수정(kakao/apple RestClient 추가)
   └─ WebConfig.java             # 신규(ArgumentResolver 등록)
```

---

## PART 2 실행 스텝

### 그룹 A — 도메인/엔티티 (Business Logic)
- [x] **Step 1** — `user/Provider.java`(KAKAO, APPLE), `user/Gender.java`(MALE, FEMALE, NONE) enum 생성
- [x] **Step 2** — `user/User.java` JPA 엔티티: id, provider, providerUserId, nickname, gender(기본 NONE), height, weight, profileImageUrl(보류 null), onboardingCompleted(기본 false), createdAt/updatedAt(@PrePersist/@PreUpdate 또는 Auditing). `@Table(uniqueConstraints=(provider, providerUserId))`. [US-USER-1/2]
- [x] **Step 3** — `user/NicknameGenerator.java`: 소셜 닉네임 우선, 없으면 `러너`+랜덤4자리(BR-5.2/5.3). [US-AUTH-1]

### 그룹 B — 토큰·해시 (Business Logic, U1-b)
- [x] **Step 4** — `auth/JwtProperties.java`(@ConfigurationProperties "jwt": secret, accessExpiration, refreshExpiration[ms])
- [x] **Step 5** — `auth/JwtProvider.java`(jjwt HS256): issueAccessToken/issueRefreshToken(sub/type/iat/exp), parseAndValidate(token, expectedType), getUserId. 만료/서명/type 불일치 → 인증 예외. secret 공백 fail-fast. accessTokenExpiresInSeconds() 노출. [US-AUTH-2]
- [x] **Step 6** — `auth/TokenHasher.java`(SHA-256 hex) + `auth/RefreshTokenStore.java`(StringRedisTemplate): save(userId,raw→해시,TTL 7d) / matches(userId,raw) / delete(userId). [US-AUTH-2/3/5, BR-2]

### 그룹 C — 소셜 검증 (external/oauth)
- [x] **Step 7** — `external/oauth/OAuthUser.java`(record: provider, providerUserId, nickname) + `OAuthClient.java`(interface: Provider provider(); OAuthUser verify(String credential)) + `OAuthProperties.java`(@ConfigurationProperties "oauth": kakao.userInfoUri, apple.jwksUri/issuer/audience)
- [x] **Step 8** — `external/oauth/dto/KakaoUserResponse.java` + `KakaoOAuthClient.java`: kakaoRestClient로 사용자 조회(Bearer credential), Resilience4j(oauthKakaoUserInfo). 실패/무효 → BusinessException(UNAUTHORIZED). [US-AUTH-1, BR-4.2]
- [x] **Step 9** — `external/oauth/AppleJwksProvider.java`: appleRestClient로 JWKS fetch + **인메모리 캐시(TTL 6h)** + kid 매칭(미스 시 강제 리프레시 1회), Resilience4j(oauthAppleJwks). [BR-4.3]
- [x] **Step 10** — `external/oauth/AppleOAuthClient.java`(nimbus-jose-jwt): identity token RS256 서명검증 + iss/aud/exp 검증 → OAuthUser(APPLE, sub, null). 실패 → UNAUTHORIZED. [US-AUTH-1, BR-4.3]
- [x] **Step 11** — `external/oauth/OAuthClientResolver.java`: provider(kakao/apple)→구현체 매핑. 미지원 → BusinessException(BAD_REQUEST). [BR-4.1]

### 그룹 D — Repository
- [x] **Step 12** — `user/UserRepository.java`(JpaRepository): findByProviderAndProviderUserId. [US-AUTH-1]

### 그룹 E — 서비스 (Business Logic)
- [x] **Step 13** — `auth/AuthService.java`: login(provider, credential)=resolve→verify→upsert(신규 시 NicknameGenerator+기본값)→JwtProvider 2토큰→RefreshTokenStore.save→onboardingRequired=!onboardingCompleted. refresh(token)=검증(REFRESH)+matches→회전. logout(userId)=delete. [US-AUTH-1/2/3/5]
- [x] **Step 14** — `user/UserService.java`: getMyProfile(userId)[없으면 NOT_FOUND], updateProfile(userId, cmd)[부분갱신 + onboardingCompleted=true], deleteAccount(userId)[하드삭제 + refresh delete]. [US-USER-1/2/3]

### 그룹 F — API Layer + DTO
- [x] **Step 15** — auth DTO: `LoginRequest`(authorizationCode @NotBlank), `RefreshRequest`(refreshToken @NotBlank), `TokenResponse`(accessToken, refreshToken, tokenType, accessTokenExpiresIn, Boolean onboardingRequired, UserProfileResponse user; @JsonInclude NON_NULL)
- [x] **Step 16** — user DTO: `UserProfileResponse`(id, nickname, gender, height, weight, profileImageUrl), `UpdateProfileRequest`(nickname, gender, height, weight — 전부 optional + 범위/길이 검증 BR-6.4)
- [x] **Step 17** — `auth/AuthController.java`: POST /auth/login/{provider}(200 TokenResponse), POST /auth/refresh(200), POST /auth/logout(@AuthUser, 204). [US-AUTH-1/3/5]
- [x] **Step 18** — `user/UserController.java`: GET /users/me(200), PATCH /users/me(200), DELETE /users/me(204). @AuthUser 주입. [US-USER-1/2/3]

### 그룹 G — 보안 인프라 배선 (U1-b)
- [x] **Step 19** — `auth/JwtAuthenticationFilter.java`(OncePerRequestFilter): Bearer 파싱→access 검증→SecurityContext 주입. 실패는 컨텍스트 미설정(EntryPoint가 401). [US-AUTH-4]
- [x] **Step 20** — `auth/RestAuthenticationEntryPoint.java`(401)/`auth/RestAccessDeniedHandler.java`(403): 공통 ApiResponse JSON 직렬화. [US-AUTH-4]
- [x] **Step 21** — `auth/AuthUser.java`(annotation) + `auth/AuthUserArgumentResolver.java`: SecurityContext에서 userId(Long) 주입
- [x] **Step 22** — `config/SecurityConfig.java` **수정**: permitAll 껍데기 → deny-by-default + 화이트리스트(패턴 §5) + JwtAuthenticationFilter addFilterBefore + EntryPoint/AccessDeniedHandler. [US-AUTH-4]
- [x] **Step 23** — `config/WebConfig.java` 신규: AuthUserArgumentResolver 등록. `config/RestClientConfig.java` **수정**: kakaoRestClient/appleRestClient 빈 추가(타임아웃)

### 그룹 H — 빌드/설정
- [x] **Step 24** — `build.gradle` **수정**: jjwt-api/impl/jackson(0.12.x) + nimbus-jose-jwt 추가(주석 해제/신규). 호환성 주석(Boot 4/Jackson 3).
- [x] **Step 25** — `application.properties` **수정**: jwt.access-expiration 3600000→**86400000(24h)** 교정, oauth.* 설정 추가, resilience 인스턴스 oauthKakaoUserInfo/oauthAppleJwks 추가. `.env.example` **수정**: APPLE_CLIENT_ID 추가

### 그룹 I — 테스트 (순수 단위 우선, Mockito)
- [x] **Step 26** — `JwtProviderTest`(발급/검증/만료/type 불일치), `TokenHasher`/`RefreshTokenStoreTest`(mock StringRedisTemplate: save/matches/delete/회전), `NicknameGeneratorTest`
- [x] **Step 27** — `AuthServiceTest`(login 신규/기존·onboardingRequired, refresh 회전·불일치 401, logout delete — mock 의존), `UserServiceTest`(getProfile 404, updateProfile 부분갱신+completed, delete), `OAuthClientResolverTest`(미지원 400)
- [x] **Step 28** — `AuthController`/`UserController` 슬라이스 또는 검증 테스트(입력 검증 400) — 기존 컨트롤러 테스트 스타일 참고(웹 슬라이스 가능 범위)

### 그룹 J — 문서
- [x] **Step 29** — `aidlc-docs/construction/u4-auth-user/code/code-summary.md`: 생성/수정 파일, 스토리 매핑, 설계 반영, 검증 결과, 호환성 노트(jjwt/Jackson3), 만료 교정 기록

---

## 완료 기준
- compileJava + compileTestJava 성공, U4 순수 단위테스트 통과.
- 8개 스토리 구현. deny-by-default 화이트리스트 적용(기존 공개 API 회귀 없음).
- 시크릿 env(코드/VCS 미저장), 토큰/credential 미로깅.

## 진행 상태
- [x] Part 1: 플랜 작성 (이 파일) — 승인됨
- [x] Part 2: 29 스텝 실행 완료 — compileJava/compileTestJava 성공, U4 단위테스트 35건 전부 통과(auth/user/oauth). code-summary.md 작성.
- 스토리 [x]: US-AUTH-1/2/3/4/5, US-USER-1/2/3 (전부 구현)
