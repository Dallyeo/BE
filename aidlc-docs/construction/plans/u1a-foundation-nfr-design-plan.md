# NFR Design Plan — U1-a 공개 기반

NFR 요구를 **설계 패턴 + 논리 컴포넌트**로 구체화하기 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **전제(이미 확정)**: Resilience4j 수치(connect2s/read3s/재시도1/서킷50%·10s, N2), Redis TTL 30분+(BR-6/F6), env 시크릿(N3), 구조화 로깅(N6), RestClient+CompletableFuture(D4). 아래 질문은 **"어떻게 구현·배선하느냐"**의 남은 선택만 다룹니다.

---

## Part A. 실행 체크리스트
- [x] NFR 요구 재분석 (성능/가용성/보안/관측성)
- [x] 결정 질문 수집(Part B) — D1=A, D2=B, D3=A, D4=A, D5=A, D6=A
- [x] `nfr-design-patterns.md` 생성 — 회복성/캐시/보안/관측 패턴
- [x] `logical-components.md` 생성 — TourApiClient/캐시/회복성/로깅 논리 컴포넌트 배선

---

## Part B. 결정 질문

### Question D1 — Resilience4j 적용 방식
타임아웃/재시도/서킷을 코드에 어떻게 얹을까요?

A) **어노테이션 기반** — `@CircuitBreaker`+`@Retry`+`@TimeLimiter`를 TourApiClient 메서드에 선언, 설정은 `application.properties` (권장, 간결·설정 외부화)
B) **프로그래매틱 데코레이터** — `Resilience4j` 데코레이터를 코드에서 조립 (세밀 제어, 보일러플레이트↑)
C) 추천에 위임
X) Other

[Answer]: A

---

### Question D2 — Resilience4j 인스턴스 범위
서킷/재시도 인스턴스를 어느 단위로 둘까요?

A) **TourAPI 단일 인스턴스** — 모든 TourAPI 엔드포인트가 하나의 서킷 공유 (권장, 외부 호스트 장애를 한 곳에서 차단)
B) **엔드포인트별 인스턴스** — areaBasedList/locationBasedList/detail 등 각각 별도 서킷 (한 API만 죽어도 나머지 유지, 설정↑)
C) 추천에 위임
X) Other

[Answer]: B

---

### Question D3 — 캐시 구현 패턴
Redis 캐시를 어떤 방식으로 다룰까요?

A) **Spring Cache 추상화(@Cacheable)** — `@EnableCaching`+RedisCacheManager, 키/TTL은 설정으로 (권장, 선언적·단순)
B) **RedisTemplate 직접 제어** — 캐시 키/직렬화/부분갱신을 수동 관리 (유연, 코드↑)
C) 추천에 위임
X) Other

[Answer]: A

---

### Question D4 — 캐시 실패/열화 모드(외부 장애 시)
TourAPI 실패 + 캐시에 값이 있을 때/없을 때 동작은?

A) **캐시 HIT는 항상 서빙, MISS+외부실패는 `EXTERNAL_API_ERROR`(502)** — stale 재사용 없음, 단순·명확 (권장)
B) **stale-on-error** — TTL 만료된 캐시라도 외부 실패 시 만료값 폴백 서빙(가용성↑, 신선도↓, 별도 저장 필요)
C) 추천에 위임
X) Other

[Answer]: A

---

### Question D5 — 민감정보 로그 마스킹 구현
serviceKey/토큰이 로그에 남지 않게 하는 방식은?

A) **호출 지점 마스킹** — 외부 URL 로깅 시 serviceKey 파라미터를 마스킹 유틸로 제거/치환 후 기록 (권장, 범위 명확·단순)
B) **Logback 패턴 컨버터** — 전역 로그 마스킹 컨버터로 패턴 매칭 치환 (누락 방지, 오탐/성능 주의)
C) A+B 병행
X) Other

[Answer]: A

---

### Question D6 — 병렬 호출 스레드 격리(벌크헤드)
상세 조합(U3에서 4콜 병렬, CompletableFuture)용 실행기 전략은? (U1-a는 기반만 마련)

A) **전용 스레드풀 Bean** — TourAPI 병렬 호출 전용 `Executor`(고정 크기) 정의, 서블릿 스레드와 격리 (권장, 벌크헤드 효과)
B) **공용 ForkJoinPool** — `CompletableFuture` 기본 풀 사용(간단, 격리 약함)
C) 추천에 위임 / U3에서 확정
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u1a-foundation/nfr-design/nfr-design-patterns.md`
- [x] `aidlc-docs/construction/u1a-foundation/nfr-design/logical-components.md`
