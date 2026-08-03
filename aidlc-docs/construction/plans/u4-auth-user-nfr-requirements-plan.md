# U4 인증·사용자 🔒 — NFR Requirements Plan

> Functional Design(U4) 확정 위에서 비기능 요구·기술선택. U1-a 스택(Spring Boot 4.0.6 / RestClient / Resilience4j / Redis / jjwt / env 시크릿) 및 U2·U3 NFR 상속.
> U4는 **첫 인증 유닛** → 보안·회복성 결정이 핵심.

---

## Part 1 — 질문 (아래 [Answer]: 태그를 채워주세요)

> 각 질문에 **권장안** 표기. 이견 없으면 권장안을 적거나 "전부 권장안"이라고만 답하셔도 됩니다.

### Question 1 — Apple identity token 검증 방식/라이브러리
Q1=B로 Apple을 실제 구현합니다. Apple `identity token`(RS256 JWT)을 Apple JWKS(공개키)로 검증해야 합니다. 어떻게?

A) **nimbus-jose-jwt 추가** — JWKS fetch·kid 선택·키 로테이션·RS256 검증을 견고하게 처리(캐시 포함). Kakao는 access token 조회(별도 검증 불필요) — **(권장: Apple JWK 처리 표준적·안전)**
B) **jjwt만 사용** — Apple JWKS를 직접 fetch/파싱해 공개키 구성 후 jjwt로 검증(의존성 추가 없음, 구현량↑)
C) Apple은 이번엔 검증 스텁(로컬), 실제 JWK 검증은 다음 반복으로 미룸
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 2 — 인증 엔드포인트 남용/브루트포스 방지(레이트리밋)
`POST /auth/login/*`, `POST /auth/refresh`에 레이트리밋을 이번 유닛에 넣을까요?

A) **이번엔 미적용(후순위)** — 소규모·초기 사용자, 서버측 refresh 회전/검증으로 재발급 남용은 이미 제한. 필요 시 후속 추가 — **(권장: 현 규모에 과설계 회피, N2 하드타깃 미설정 기조와 일치)**
B) **간단 레이트리밋 적용** — Resilience4j RateLimiter 또는 Bucket4j로 IP/계정 단위 제한(로그인/갱신)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 3 — Redis Refresh Token 저장 형태
`refresh:{userId}` 값을 어떤 형태로 저장할까요? (Q6=A 회전 정책 전제)

A) **해시(SHA-256)로 저장** — 갱신 시 전달 토큰의 해시와 대조. Redis 유출 시에도 원문 노출 방지(방어적) — **(권장: Security Baseline 정합, 구현 부담 적음)**
B) **평문 토큰 저장** — 대조 단순, Redis는 내부망 코로케이션. 구현 최소
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 4 — 소셜 외부 호출 회복성
Kakao 사용자 조회 API / Apple JWKS 호출에 U1-a Resilience4j 패턴을 적용할까요?

A) **적용(권장)** — 기존 패턴 재사용: connect 2s / read 3s 타임아웃 + 재시도1 + 서킷. Apple JWKS는 캐시(키 로테이션 대비 TTL). 실패 → 401/외부오류 매핑
B) **최소** — 타임아웃만 설정, 서킷/재시도 미적용
X) Other (please describe after [Answer]: tag below)

[Answer]: A

### Question 5 — 저장 데이터 보호(SECURITY-01 at-rest) 스탠스
U4부터 개인정보(닉네임/성별/키/체중 + 소셜 ID)가 MySQL에 저장됩니다. 전송구간(TLS)은 nginx가 처리. **저장 시 암호화(at-rest)** 스탠스는?

A) **현 단계 수용(문서화된 accepted risk)** — 단일 EC2 코로케이션, 관리형 키서비스 미도입. 애플리케이션 하드코딩 시크릿 없음(env). 추후 RDS 이관 시 스토리지 암호화·TLS 활성 — **(권장: 현 인프라 현실 반영, 리스크 명시)**
B) **지금 디스크/DB 암호화 구성** — MySQL 저장 암호화 또는 EBS 암호화 등 인프라 작업 포함
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

## 고정 사항 (질문 아님 — 스토리/이전 결정에서 확정)
- 토큰 만료: Access 24h / Refresh 7d (US-AUTH-2). 서명 HS256, env `JWT_SECRET`.
- 비밀번호 없음(소셜 전용) → 패스워드 해싱 N/A.
- JWT 라이브러리: jjwt(U1-a tech-stack 확정) — 자체 발급/검증.
- 공개 화이트리스트 deny-by-default(SECURITY-08) — Functional Design 반영 완료.
- 로깅: 토큰/credential 미로깅(SECURITY-03, LogMaskingUtil 상속).

---

## Part 2 — NFR 산출물 생성 계획 (답변 확정 후 실행)
- [x] `u4-auth-user/nfr-requirements/nfr-requirements.md` — 확장성/성능/가용성·신뢰성/보안(베이스라인 매핑표)/유지보수·관측성/N-A
- [x] `u4-auth-user/nfr-requirements/tech-stack-decisions.md` — jjwt + nimbus(Apple JWK) + RestClient(Kakao) + Resilience4j + Redis 해시 refresh + 의존성/시크릿

---

## 진행 상태
- [x] Step 1: Functional Design 분석
- [x] Step 2~4: 플랜 + 질문 작성·저장 (이 파일)
- [x] Step 5: 답변 수집 및 모호성 분석 (Q1~Q5 전부 A — 모호성 없음)
- [x] Step 6: NFR 산출물 2종 생성
- [ ] Step 7~9: 완료 메시지(제시) · 승인 대기 · aidlc-state 갱신
