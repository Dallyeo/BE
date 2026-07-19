# NFR Requirements Plan — U1-a 공개 기반

U1-a의 비기능 요구(NFR) + 기술스택 결정을 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **U1-a 성격**: 외부 TourAPI 연동 기반 + 공통 응답/예외 + Security 껍데기. 성능/회복성/보안(시크릿)이 핵심.
> **Security Baseline 활성** — 시크릿 관리·전송 보안 항목 포함(필수).
> 이미 확정: 캐시 Redis TTL 30분+(F6=C), 회복성 Resilience4j(D7), permitAll 껍데기(U1-a).

---

## Part A. 실행 체크리스트
- [x] NFR 요구 정리 (`nfr-requirements.md`) — 성능/가용성/보안/신뢰성/유지보수
- [x] 기술스택 결정 (`tech-stack-decisions.md`)

**확정 답변**: N1=A(소규모), N2=A(표준 회복성 수치), N3=A(serviceKey 환경변수), N4=C(성능 하드타깃 미설정·기능 우선), N5=A(jjwt), N6=A(구조화 로깅+외부호출 로깅).

---

## Part B. 결정 질문

### Question N1 — 예상 규모/부하 (성능 목표 산정용)
초기 서비스 규모를 어떻게 볼까요?

A) **소규모** — 동시 사용자 수십~수백, 지역(전북) 대상. 단일 인스턴스로 충분 (권장, 현실적)
B) **중간** — 동시 수천, 확장 대비 필요
C) 모름 — 일단 소규모로 잡고 이후 조정
X) Other

[Answer]: A

---

### Question N2 — 외부 연동 회복성 수치 (Resilience4j)
TourAPI 호출의 타임아웃/재시도/서킷을 어떻게 잡을까요?

A) **표준값** — connect 2s / read 3s, 재시도 1회(지수백오프), 서킷 실패율 50%·open 10s (권장)
B) **더 관대하게** — read 5s+, 재시도 2회 (외부 느림 감수)
C) **더 엄격하게** — read 2s, 재시도 0 (빠른 실패)
X) Other

[Answer]: A

---

### Question N3 — TourAPI serviceKey/시크릿 관리 (Security Baseline)
외부 인증키를 어떻게 보관할까요?

A) **환경변수 주입** — `application.properties`는 `${TOURAPI_SERVICE_KEY}` 참조, 실제 값은 env/로컬 `.env`(git 제외) (권장, 단순·안전)
B) **시크릿 매니저** — AWS Secrets Manager 등 외부 시크릿 저장소
C) properties에 직접(비권장)
X) Other

[Answer]: A

---

### Question N4 — 성능 목표 (응답시간)
장소 조회 응답시간 목표는? (캐시 HIT/MISS 기준)

A) **캐시 HIT < 100ms, MISS(외부 1콜) < 1.5s, 상세(병렬 조합) < 2s** (권장)
B) 더 여유롭게 (MISS < 3s)
C) 목표 미설정(초기엔 기능 우선)
X) Other

[Answer]: C

---

### Question N5 — JWT 라이브러리 선택 (U1-b에서 사용, 지금 결정)
토큰 발급/검증 라이브러리는?

A) **jjwt (io.jsonwebtoken)** — 국내외 Spring 예제 다수, 간단 (권장)
B) **nimbus-jose-jwt** — OIDC/JWK 등 고급 기능(애플 로그인 검증에 유리)
C) 추천에 위임
X) Other

[Answer]: A

---

### Question N6 — 로깅/관측성 수준
초기 로깅/모니터링 수준은?

A) **구조화 로깅 + 요청/외부호출 로깅** — 표준 로깅에 외부 API 실패/캐시 히트율/미매핑 WARN 포함 (권장)
B) **기본 로깅만** — 프레임워크 기본
C) APM 도입(Datadog/Prometheus 등) — 초기엔 과함
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u1a-foundation/nfr-requirements/nfr-requirements.md`
- [x] `aidlc-docs/construction/u1a-foundation/nfr-requirements/tech-stack-decisions.md`
