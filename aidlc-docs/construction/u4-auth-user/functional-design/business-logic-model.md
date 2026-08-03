# U4 — Business Logic Model (인증·사용자)

> 기술 비의존 흐름 설계. U1-b(JWT 발급/검증, 인증 필터)는 auth 로직과 밀접하여 본 문서에 통합.
> 컴포넌트(참조: application-design/component-methods.md): `AuthController/AuthService`, `OAuthClient(Kakao/Apple)`, `JwtProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, `UserController/UserService/UserRepository`, `RefreshTokenStore`.

---

## 0. 컴포넌트 책임

| 컴포넌트 | 책임 |
|---|---|
| `SecurityConfig` | deny-by-default + 공개 화이트리스트, `JwtAuthenticationFilter` 등록, stateless |
| `JwtAuthenticationFilter` | `Authorization: Bearer` 파싱 → JwtProvider 검증 → SecurityContext에 userId 주입 |
| `JwtProvider` | Access/Refresh 발급, 서명·만료·type 검증, `getUserId` 추출 |
| `OAuthClient` (인터페이스) | `OAuthUser verify(String credential)` — 제공자별 소셜 검증. 구현체: `KakaoOAuthClient`, `AppleOAuthClient` |
| `RefreshTokenStore` | Redis refresh 저장/대조/삭제(회전) |
| `AuthService` | 로그인/가입, 갱신, 로그아웃 오케스트레이션 |
| `UserService` | 프로필 조회/수정, 계정 삭제 |
| `AuthArgumentResolver`(또는 `@AuthUser`) | 컨트롤러에서 현재 userId 주입 |

> **OAuthClient 추상화 노트(Q2)**: 현재는 A안(프론트가 access token/identity token 전달 → 검증)으로 구현. 훗날 B안(authorization code 교환)으로 전환 시 **해당 제공자 구현체 내부만 교체**(code→token 교환 단계 추가 + client_secret/redirect_uri 설정). Controller/Service/DTO/프론트 계약(`authorizationCode` 필드) 불변.

---

## 1. 소셜 로그인/가입 — `POST /auth/login/{provider}` (US-AUTH-1, US-AUTH-2)

**입력**: path `provider`(kakao|apple), body `{ authorizationCode }`

```
1. provider 파싱: "kakao"→KAKAO, "apple"→APPLE. 미지원 값 → 400.
2. OAuthClient 선택(provider별) → verify(authorizationCode)
   - Kakao(A안): 전달받은 카카오 access token으로 카카오 사용자 조회 API 호출 → providerUserId, nickname
   - Apple(A안): identity token(JWT) 서명을 Apple 공개키로 검증 → sub(providerUserId). nickname 없음.
   - 검증 실패(만료/위조/외부오류) → 401 (인증 실패)
3. User upsert:
   - findByProviderAndProviderUserId 존재 → 기존 User (로그인)
   - 없음 → 신규 User 생성:
       nickname = OAuthUser.nickname ?? 자동생성("러너"+랜덤4자리)
       gender = NONE, height/weight = null, onboardingCompleted = false
   - isNew 플래그 보관
4. 토큰 발급: JwtProvider.issueAccessToken(userId), issueRefreshToken(userId)
5. RefreshTokenStore.save(userId, refreshToken)  // refresh:{userId}, TTL 7d (기존 값 덮어씀)
6. onboardingRequired = !user.onboardingCompleted   // Q8=B
7. 응답: accessToken, refreshToken, tokenType="Bearer",
         accessTokenExpiresIn=86400, onboardingRequired, user(프로필)
```

**응답 200** (api-spec §1.1). 신규/기존 모두 200(생성 여부는 onboardingRequired로 구분).

---

## 2. 토큰 갱신 — `POST /auth/refresh` (US-AUTH-3) 🌐

**입력**: body `{ refreshToken }`

```
1. JwtProvider 검증: 서명·만료·type=="refresh" 확인. 실패 → 401.
2. userId = JwtProvider.getUserId(refreshToken)
3. RefreshTokenStore 대조: stored = get(refresh:{userId})
   - stored 없음(만료/로그아웃) 또는 stored != 전달 refreshToken → 401 (재로그인 유도)
4. 회전(rotation):
   - newAccess = issueAccessToken(userId)
   - newRefresh = issueRefreshToken(userId)
   - RefreshTokenStore.save(userId, newRefresh)   // 이전 refresh 즉시 무효
5. 응답: newAccess, newRefresh, tokenType, accessTokenExpiresIn=86400
```

> 공개(🌐) 엔드포인트: access token 없이 접근 가능(만료 상황 전제). 검증은 refresh 자체 + Redis 대조로 수행.

---

## 3. 로그아웃 — `POST /auth/logout` (US-AUTH-5) 🔒

**입력**: `Authorization: Bearer {accessToken}` (userId는 토큰에서)

```
1. 필터가 access token 검증 → userId 주입
2. RefreshTokenStore.delete(refresh:{userId})   // 서버측 refresh 무효화
3. 응답 204 (본문 없음)
```
- Access Token은 stateless라 만료 전까지 유효(블랙리스트 미도입) → 클라이언트가 토큰 폐기. 서버는 refresh 차단으로 재발급 경로를 끊음(24h 내 자연 만료).

---

## 4. 인증 미들웨어 (US-AUTH-4 · U1-b) 🔧

### 4.1 SecurityConfig — deny-by-default + 화이트리스트
U1-a의 `anyRequest().permitAll()` 껍데기를 **교체**:
```
- csrf off, httpBasic/formLogin off, session STATELESS (기존 유지)
- 공개 화이트리스트(permitAll):
    POST /auth/login/*        (소셜 로그인)
    POST /auth/refresh        (토큰 갱신)
    GET  /regions
    GET  /courses, GET /courses/{id}
    GET  /places/**           (search, 목록, nearby, {id})
    (인프라) /actuator/health  등 헬스체크
- anyRequest().authenticated()   ← 그 외 전부 인증
- addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)
```
> 화이트리스트 근거: auth-classification.md §1 + unit-of-work-dependency §3. `POST /auth/logout`, `/users/**`, (U5) `/runs/**`, `POST /courses`는 화이트리스트 제외 → 인증 필요.

### 4.2 JwtAuthenticationFilter
```
1. Authorization 헤더에서 "Bearer " 추출. 없으면 → 필터 통과(인증 미설정) → 보호 경로면 EntryPoint가 401
2. JwtProvider 검증(서명/만료/type=="access"). 실패 → 401(인증 실패 응답), 체인 중단
3. 성공 → userId로 Authentication 구성, SecurityContext에 설정
4. 체인 계속
```
- 인증 실패/미인증 진입점(`AuthenticationEntryPoint`) → 공통 `ApiResponse` 형식 401.
- 접근 거부(`AccessDeniedHandler`) → 403.

### 4.3 현재 사용자 주입
- 컨트롤러는 `@AuthUser Long userId`(또는 ArgumentResolver)로 SecurityContext의 userId 획득 → Service에 전달.

---

## 5. 프로필 조회 — `GET /users/me` (US-USER-2) 🔒
```
1. 필터로 userId 확보
2. UserRepository.findById(userId) → 없으면 404(비정상: 토큰 유효한데 삭제된 계정)
3. UserProfileDto 매핑 반환 (id, nickname, gender, height, weight, profileImageUrl=null)
```

---

## 6. 프로필/온보딩 수정 — `PATCH /users/me` (US-USER-1, US-USER-2) 🔒
> 온보딩 신체정보 저장과 설정 수정 공용. 부분 갱신(전달된 필드만).

```
1. userId 확보 → User 로드(없으면 404)
2. UpdateProfileCommand의 non-null 필드만 반영:
   - nickname 있으면 갱신
   - gender 있으면 갱신(enum 유효성)
   - height 있으면 갱신(유효 범위)
   - weight 있으면 갱신(유효 범위)
3. onboardingCompleted = true 로 설정   // Q8=B: 온보딩 화면 완료(저장 또는 건너뛰기) 시점
   - 건너뛰기: 프론트가 빈 바디 PATCH 호출 → 신체정보 미변경, completed만 true
4. updatedAt 갱신, 저장
5. 수정된 UserProfileDto 반환(200)
```
> PATCH가 온보딩 "완료" 신호를 겸함(별도 skip API 없음). 저장이든 건너뛰기든 PATCH 1회 = 온보딩 종료.

---

## 7. 계정 삭제 — `DELETE /users/me` (US-USER-3) 🔒
```
1. userId 확보
2. UserRepository.delete(userId)  // 하드 삭제(Q7=A)
3. RefreshTokenStore.delete(refresh:{userId})  // 세션 정리
4. 응답 204
```
> 현재 User는 FK 의존 없음. U5(runs/courses) 도입 시 cascade/사전삭제 규칙을 U5 설계에서 확장.

---

## 8. 흐름 요약 (텍스트 시퀀스)

```
[로그인] FE --(provider, credential)--> AuthController --> AuthService
   --> OAuthClient.verify --> (Kakao/Apple 외부) --> OAuthUser
   --> UserRepository.upsert --> JwtProvider.issue x2 --> RefreshTokenStore.save
   --> TokenResponse(+onboardingRequired,+user)

[보호 API] FE --(Bearer access)--> JwtAuthenticationFilter.검증 --> SecurityContext(userId)
   --> Controller(@AuthUser) --> Service

[갱신] FE --(refresh)--> AuthService: JWT검증 + Redis대조 --> 회전 --> TokenResponse
[로그아웃] FE --(Bearer)--> RefreshTokenStore.delete --> 204
```
