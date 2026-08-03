# U4 인증·사용자 🔒 — Functional Design Plan

> 유닛 U4 = **auth + user 도메인** (+ U1-b: JWT/필터/OAuthClient 함께 착수)
> 스토리: US-AUTH-1(소셜로그인) · US-AUTH-2(토큰 발급/만료) · US-AUTH-3(토큰 갱신) · US-AUTH-4(인증 미들웨어) · US-AUTH-5(로그아웃) · US-USER-1(온보딩 신체정보) · US-USER-2(프로필 조회/수정) · US-USER-3(계정 삭제)
> 엔드포인트: `POST /auth/login/{provider}` · `POST /auth/refresh` · `POST /auth/logout` · `GET /users/me` · `PATCH /users/me` · `DELETE /users/me`

---

## Part 1 — 질문 (아래 [Answer]: 태그를 채워주세요)

> 답변 방식: 각 질문의 `[Answer]:` 뒤에 **선택지 문자(A/B/…)** 를 적어주세요. `X) Other` 선택 시 설명을 함께 적어주세요.
> 각 질문에 **권장안**을 표기했습니다. 특별한 이견이 없으면 권장안을 그대로 적으셔도 됩니다.

### Question 1 — Kakao/Apple 실제 구현 범위
이번 U4에서 소셜 제공자를 어디까지 실제 연동할까요? (군산 로컬 서비스, 초기 사용자)

A) **Kakao만 실제 구현**, Apple은 인터페이스/분기만 두고 미구현(추후) — **(권장: 가장 빠른 실사용 경로)**
B) Kakao + Apple **둘 다 실제 구현** (Apple은 identity token JWT 검증 + Apple 공개키)
C) 둘 다 인터페이스만, 실제 검증은 목/스텁 (로컬 개발용)
X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 2 — 소셜 검증 방식 (프론트가 무엇을 보내는가)
`POST /auth/login/{provider}` Request의 `authorizationCode`가 무엇이며 백엔드가 어떻게 검증하나요?

A) **Kakao: 프론트가 받은 Kakao access token을 전송** → 백엔드가 카카오 `/v2/user/me` 호출로 검증·사용자정보 조회. **Apple: identity token(JWT)** 전송 → 백엔드가 Apple 공개키로 서명 검증 — **(권장: 프론트 SDK 로그인 후 토큰 전달, 백엔드 단순)**
B) Kakao/Apple 모두 **인가 코드(authorization code)** 전송 → 백엔드가 토큰 엔드포인트에 code 교환(client_secret 필요) 후 사용자정보 조회
C) 혼합(제공자별 다름) — 상세는 Other에 기술
X) Other (please describe after [Answer]: tag below)

[Answer]: A 우선 A로 진행하는데 나중에 바꿀 수 있는지도 확인 한번 해줘
확인하고 말해줘 이거 읽을 때

### Question 3 — 사용자 식별 & 재로그인 매칭 키
동일 사용자를 어떻게 식별/재로그인 매칭하나요? (User 엔티티 유니크 키)

A) **`provider` + `providerUserId`(소셜 고유 ID) 복합 유니크** — 이메일 미수집. **(권장: Apple 이메일 비공개·릴레이 대응, 최소 수집)**
B) **이메일** 기준 매칭 (제공자 달라도 같은 이메일이면 동일 계정)
C) provider+providerUserId 유니크 + 이메일도 저장(참고용, 매칭엔 미사용)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4 — 신규 가입 시 nickname 처리
최초 로그인(신규) 시 `nickname`을 어떻게 정하나요? (Apple은 닉네임 미제공/최초 1회만)

A) **소셜 프로필 닉네임 사용, 없으면 자동생성**(예: `러너1234`) — 사용자는 이후 PATCH로 변경 — **(권장)**
B) 항상 자동생성 닉네임, 소셜 닉네임 미사용
C) nickname은 nullable로 두고 온보딩에서 입력받음
X) Other (please describe after [Answer]: tag below)

[Answer]: A 로 하는데 닉네임은 아직 얘기가 없어서 우선 A로 만들어놓고 사용할지 안할지는 결정해서 그때 사용하면 될듯
근데 A로하면 닉네임이 어떤 식으로 나오는지 설명해줘

### Question 5 — JWT 서명 방식 & 클레임
Access/Refresh 토큰 서명과 클레임 구성은?

A) **HS256 대칭키**(env `JWT_SECRET`), 클레임: `sub=userId`, `type=access|refresh`, `iat`, `exp`. Access 24h / Refresh 7d — **(권장: 단일 EC2, 자체 발급/검증만 필요)**
B) RS256 비대칭키(공개키 분리 배포 필요)
C) HS256 + 추가 클레임(nickname, roles 등)도 토큰에 포함
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 6 — Refresh Token 저장/회전 정책 (Redis)
`/auth/refresh`가 새 refreshToken도 반환합니다. 저장·회전 정책은?

A) **사용자당 1개 세션**: Redis 키 `refresh:{userId}` = 현재 refresh 토큰(또는 해시), TTL 7d. 갱신 시 **회전(rotation)** — 새 토큰으로 교체하고 이전 토큰 무효. 로그아웃 시 키 삭제. **(권장: 단순·안전, 한 기기 로그인 가정)**
B) **다중 기기**: 사용자당 여러 refresh 토큰 허용(`refresh:{userId}:{tokenId}` 집합). 로그아웃은 해당 기기만 무효
C) 회전 없이 만료까지 동일 refresh 재사용 (갱신 시 access만 재발급, refresh 그대로)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 7 — 계정 삭제(US-USER-3) 방식
`DELETE /users/me` 처리 방식은? (U5에서 runs가 user를 참조할 예정)

A) **하드 삭제**: user 레코드 삭제 + Redis refresh 삭제. (연관 데이터는 U5에서 FK/cascade 설계 시 함께 정리) — **(권장: 현재 연관 데이터 없음, 단순)**
B) **소프트 삭제**: `deleted`/`deletedAt` 플래그로 비활성화, 레코드 보존(감사/복구 여지). 로그인 시 비활성 계정 차단
C) 하드 삭제 + 삭제 이력만 별도 익명 로그 보관
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 8 — 온보딩 완료(onboardingRequired) 판정 기준
로그인 응답의 `onboardingRequired`는 무엇을 기준으로 계산하나요?

A) **height 또는 weight가 null이면 true** (신체정보 미입력 = 온보딩 필요) — **(권장: 별도 플래그 불필요)**
B) 별도 `onboardingCompleted` 플래그 컬럼 사용 (건너뛰기해도 "완료"로 표시 가능)
C) 신규 가입(최초 로그인)일 때만 true, 이후 항상 false
X) Other (please describe after [Answer]: tag below)

[Answer]: B

---

## Part 2 — Functional Design 산출물 생성 계획 (답변 확정 후 실행)

- [x] `u4-auth-user/functional-design/domain-entities.md` — User 엔티티 + Redis RefreshToken + JWT 클레임 + OAuthUser. Q7=A 하드삭제(soft 컬럼 없음), Q8=B onboardingCompleted 플래그.
- [x] `u4-auth-user/functional-design/business-logic-model.md` — 로그인/가입(OAuth 검증→upsert→토큰발급), 토큰 갱신(회전), 로그아웃(무효화), 프로필 조회/수정(PATCH 부분갱신), 계정 삭제 흐름 + JWT 발급/검증 + SecurityConfig/JwtAuthenticationFilter(deny-by-default+화이트리스트) 인증 미들웨어 + OAuthClient 추상화(Q2 전환 가능성)
- [x] `u4-auth-user/functional-design/business-rules.md` — BR-1~8: 토큰 만료(24h/7d), refresh 대조/회전, 화이트리스트, 소셜검증(Kakao/Apple), 식별키, onboardingRequired, 유효성, 에러코드 매핑

> 인증 미들웨어(U1-b)는 auth 도메인 로직과 밀접하여 본 유닛의 business-logic-model에 통합 기술.

---

## 진행 상태
- [x] Step 1: 유닛 컨텍스트 분석 (unit-of-work / story-map / auth-classification / api-spec / component-methods)
- [x] Step 2~4: 플랜 + 질문 작성·저장 (이 파일)
- [x] Step 5: 답변 수집 및 모호성 분석 (Q1=B,Q2=A,Q3=A,Q4=A,Q5=A,Q6=A,Q7=A,Q8=B — 블로킹 모호성 없음; Q2/Q4 부가 설명 제공)
- [x] Step 6: Functional Design 산출물 3종 생성
- [ ] Step 7~9: 완료 메시지(제시) · 승인 대기 · aidlc-state 갱신
