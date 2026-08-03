# U4 — Business Rules (인증·사용자)

> 결정: Q1=B(Kakao+Apple 실구현) · Q2=A(토큰 검증, 추후 code 방식 전환 가능) · Q3=A · Q4=A · Q5=A · Q6=A · Q7=A · Q8=B.

---

## BR-1. 토큰 정책
- **BR-1.1** Access Token 만료 = **24시간(86400s)**, Refresh Token 만료 = **7일(604800s)**. (US-AUTH-2)
- **BR-1.2** 서명 알고리즘 **HS256**, 시크릿 = env `JWT_SECRET`. 시크릿 미설정 시 부팅 실패(안전).
- **BR-1.3** 클레임 `type`으로 access/refresh 구분. access 자리에 refresh 사용(또는 반대) → 검증 실패(401).
- **BR-1.4** `accessTokenExpiresIn`은 응답에 초 단위(86400)로 반환.

## BR-2. Refresh Token 저장·회전 (Q6=A)
- **BR-2.1** 사용자당 유효 Refresh Token **최대 1개**. Redis 키 `refresh:{userId}`, TTL 7d.
- **BR-2.2** `/auth/refresh` 성공 시 **회전**: 새 access+refresh 발급, Redis 값 교체 → **이전 refresh 즉시 무효**.
- **BR-2.3** 갱신 요청 refresh가 Redis 저장값과 **불일치**하거나 키가 **없으면 401**(재로그인 유도). (US-AUTH-3)
- **BR-2.4** 신규 로그인 시 기존 `refresh:{userId}` 덮어씀 → 직전 기기 세션 무효(단일 세션).
- **BR-2.5** 로그아웃/계정삭제 시 `refresh:{userId}` 삭제.

## BR-3. 인증/인가 (deny-by-default) — US-AUTH-4
- **BR-3.1** 기본 정책 **모든 경로 인증 필요**. 공개(🌐)는 아래 화이트리스트만 예외.
- **BR-3.2** 공개 화이트리스트(auth-classification §1):
  `POST /auth/login/*`, `POST /auth/refresh`, `GET /regions`, `GET /courses`, `GET /courses/{id}`, `GET /places/**`, (인프라)`/actuator/health`.
- **BR-3.3** 보호 엔드포인트에 토큰 없음/만료/위조 → **401**.
- **BR-3.4** 소유 리소스 접근 시 토큰 userId ≠ 리소스 소유자 → **403/404**. (U4 users/me는 본인 고정이라 자연 충족; runs는 U5)
- **BR-3.5** Access Token은 stateless — 로그아웃 후에도 만료 전까지 서버는 개별 무효화하지 않음(refresh 차단으로 재발급 경로 차단). 블랙리스트 미도입(범위 밖).

## BR-4. 소셜 검증 (Q1=B, Q2=A)
- **BR-4.1** 지원 provider: `kakao`, `apple`. 그 외 path 값 → **400**.
- **BR-4.2** Kakao: 전달 credential(access token)로 카카오 사용자 조회 성공 시에만 로그인. 실패/무효 → **401**.
- **BR-4.3** Apple: identity token(JWT) 서명을 Apple 공개키(JWKS)로 검증 + `iss`/`aud`/만료 확인. 실패 → **401**.
- **BR-4.4** 외부 소셜 서버 오류/타임아웃 → 인증 실패(4xx/5xx 구분은 NFR 단계에서 회복성과 함께 확정). 기본 401 취급.
- **BR-4.5** (확장) A→B(authorization code 교환) 전환은 제공자 구현체 내부 변경만으로 가능 — 외부 계약 불변.

## BR-5. 사용자 식별·가입 (Q3=A, Q4=A)
- **BR-5.1** 재로그인 매칭 키 = `(provider, providerUserId)` 복합 유니크. 이메일 미수집.
- **BR-5.2** 신규 사용자: 검증 결과에 소셜 닉네임 있으면 사용, 없으면 **자동생성 `러너`+랜덤4자리**(예 `러너3821`).
- **BR-5.3** 닉네임 **유니크 제약 없음**(중복 허용). 사용자는 `PATCH /users/me`로 변경 가능.
- **BR-5.4** 신규 가입 시 기본값: `gender=NONE`, `height=null`, `weight=null`, `onboardingCompleted=false`.

## BR-6. 온보딩·프로필 (Q8=B) — US-USER-1, US-USER-2
- **BR-6.1** `onboardingRequired = !onboardingCompleted` (로그인 응답에 포함).
- **BR-6.2** `PATCH /users/me` 호출 시 **항상 `onboardingCompleted=true`** 설정(저장/건너뛰기 공통 = 온보딩 종료 신호).
- **BR-6.3** PATCH는 **부분 갱신**: 전달된(non-null) 필드만 반영. 미전달 필드는 유지.
- **BR-6.4** 유효성:
  - `gender` ∈ {MALE, FEMALE, NONE} — 그 외 → 400.
  - `height` 전달 시 양수·합리 범위(예: 50~250cm) — 벗어나면 400.
  - `weight` 전달 시 양수·합리 범위(예: 20~300kg) — 벗어나면 400.
  - `nickname` 전달 시 공백 불가·길이 제한(예: 1~20자) — 위반 시 400.
- **BR-6.5** `profileImageUrl`은 항상 `null` 반환(업로드 보류 백로그).

## BR-7. 계정 삭제 (Q7=A) — US-USER-3
- **BR-7.1** `DELETE /users/me` = 하드 삭제(User 레코드 물리 삭제) + `refresh:{userId}` 삭제 → **204**.
- **BR-7.2** 삭제 후 해당 사용자의 기존 access token은 stateless라 만료(24h)까지 형식상 유효하나, 대상 리소스 부재로 실질 무력.

## BR-8. 에러 코드 매핑
| 상황 | HTTP | 비고 |
|---|---|---|
| 미지원 provider / 검증 바디 오류 / 유효성 위반 | 400 | GlobalExceptionHandler |
| 소셜 검증 실패 / 토큰 없음·만료·위조 / refresh 불일치 | 401 | 인증 실패 |
| 타인 소유 리소스 접근 | 403 | (U4는 users/me 본인 고정) |
| 존재하지 않는 사용자(토큰 유효하나 삭제됨) | 404 | |
| 로그인/갱신 성공 | 200 | TokenResponse |
| 로그아웃 / 계정삭제 성공 | 204 | 본문 없음 |

> 응답 포맷은 기존 U1-a `ApiResponse<T>` / `ApiError` 래퍼 계승. 인증 실패(EntryPoint)·접근거부(AccessDeniedHandler)도 동일 래퍼로 직렬화.

---

## 보안 베이스라인 관련 메모 (해당 규칙 상세는 NFR 단계에서 확정)
- SECURITY-03: 토큰/비밀번호/소셜 credential은 로그에 남기지 않음(기존 `LogMaskingUtil` 활용).
- SECURITY-08: deny-by-default 준수(BR-3.1). 공개 예외는 BR-3.2에 명시·사유 존재(auth-classification).
- `JWT_SECRET`·소셜 client 키는 env 시크릿(코드/리포 하드코딩 금지) — U1-a `.env` 패턴.
