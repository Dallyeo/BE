# NFR Design Patterns — U1-a 공개 기반

> 확정 답변: D1=A(어노테이션 기반), D2=**B(엔드포인트별 서킷 인스턴스)**, D3=A(@Cacheable 추상화), D4=A(stale 재사용 없음), D5=A(호출지점 마스킹), D6=A(전용 스레드풀 벌크헤드).
> 상위 근거: `nfr-requirements.md`, `tech-stack-decisions.md`, `business-rules.md`(BR-3/4/6/7).

---

## 1. 회복성 패턴 (Resilience) — D1=A, D2=B

### 1.1 적용 방식 — 어노테이션 기반 (D1=A)
- `TourApiClient`의 각 외부 호출 메서드에 Resilience4j 어노테이션 조합 선언:
  - `@TimeLimiter` — connect 2s / read 3s
  - `@Retry` — 1회, 지수 백오프, 대상: 타임아웃·5xx (4xx는 재시도 제외)
  - `@CircuitBreaker` — 실패율 50% 초과 시 open, 10s 후 half-open
- 파라미터는 **`application.properties`로 외부화**(코드 하드코딩 금지, N3/N6 관측·설정 외부화 원칙).
- 어노테이션 순서(바깥→안쪽): `Retry → CircuitBreaker → TimeLimiter`
  (재시도가 서킷/타임아웃을 감싸도록. 서킷 open 시 즉시 실패, 재시도는 열린 서킷을 두드리지 않음.)

### 1.2 인스턴스 범위 — **엔드포인트별** (D2=B)
- TourAPI 엔드포인트마다 **독립 서킷/재시도 인스턴스**를 둔다. 한 엔드포인트 장애가 나머지를 막지 않음.
- 인스턴스 네이밍(설정 키):

  | 인스턴스명 | 대상 TourAPI 오퍼레이션 | 사용 유닛 |
  |---|---|---|
  | `tourApiAreaBased` | `areaBasedList2` (지역기반 목록) | U2 |
  | `tourApiLocationBased` | `locationBasedList2` (위치기반 목록) | U2/U3 |
  | `tourApiSearchKeyword` | `searchKeyword2` (키워드 검색) | U2/U3 |
  | `tourApiDetailCommon` | `detailCommon2` | U3 |
  | `tourApiDetailIntro` | `detailIntro2` | U3 |
  | `tourApiDetailInfo` | `detailInfo2` | U3 |
  | `tourApiDetailImage` | `detailImage2` | U3 |

- U1-a는 **인스턴스 정의·설정 스캐폴딩**만 마련(공통 설정 + 명명 규칙). 실제 엔드포인트별 배선은 U2/U3에서 각 클라이언트 메서드에 부착.
- **공통 기본값(default) + 인스턴스별 오버라이드** 구조: `resilience4j.*.configs.default`에 표준값, 인스턴스는 default 상속 후 필요 시만 조정.

### 1.3 폴백 (Fallback)
- 모든 인스턴스 공통 폴백: 서킷 open / 재시도 소진 / 타임아웃 → **`EXTERNAL_API_ERROR`(502)** 로 정규화(BR-7).
- 폴백 메서드는 예외를 공통 실패 응답(`ApiResponse` 실패 래퍼)으로 변환. 내부 스택/외부 원문 미노출(보안).

---

## 2. 캐시 패턴 (Caching) — D3=A, D4=A

### 2.1 구현 — Spring Cache 추상화 (D3=A)
- `@EnableCaching` + **RedisCacheManager**. 조회 메서드에 `@Cacheable` 선언.
- **키 전략(BR-6)**: `오퍼레이션명 + 정규화된 파라미터`. 예) `tour:areaBased:{areaCode}:{sigunguCode}:{contentTypeId}:{pageNo}:{numOfRows}`.
  - 파라미터 정규화(정렬/기본값 적용) 후 키 생성 → 동일 요청 캐시 명중률↑.
- **TTL 30분+**(BR-6/F6). `RedisCacheConfiguration.entryTtl`로 지정, 캐시별 오버라이드 가능.
- **직렬화**: 키 `StringRedisSerializer`, 값 `GenericJackson2JsonRedisSerializer`(타입정보 포함, DTO 역직렬화 안전).
- **null/빈 결과 캐싱 정책**: 정상 빈 목록은 캐시(과호출 방지), **예외/실패 응답은 캐시 금지**(`unless`/캐시 예외 처리).

### 2.2 열화 모드 — stale 재사용 없음 (D4=A)
- 캐시 **HIT → 항상 서빙**(외부 장애와 무관, TTL 이내 값).
- 캐시 **MISS + 외부 실패 → `EXTERNAL_API_ERROR`(502)**. 만료값/stale 폴백 없음(단순·신선도 우선).
- 결과: 캐시는 성능·부하완화 계층일 뿐, 가용성 폴백 저장소로 쓰지 않음. (BR-7의 실패 정규화와 일관.)

---

## 3. 보안 패턴 (Security) — D5=A  [Security Baseline]

### 3.1 시크릿 관리 (N3)
- TourAPI `serviceKey`는 **환경변수 주입**(`${TOURAPI_SERVICE_KEY}`). 코드/문서/VCS 저장 금지. 로컬 `.env`(git 제외).
- `TourApiProperties`(@ConfigurationProperties)로 바인딩, 값은 런타임 env에서만.

### 3.2 로그 마스킹 — 호출지점 마스킹 (D5=A)
- 외부 호출 URL/파라미터를 로깅할 때 **마스킹 유틸**로 `serviceKey` 값을 `***`로 치환한 뒤 기록.
- 마스킹 대상: `serviceKey`, 향후 토큰/쿠키(U1-b). 유틸은 재사용 가능한 공통 컴포넌트로 배치.
- 범위 한정 방식(전역 Logback 컨버터 미사용) — 오탐/성능 부담 없이 외부호출 로깅 지점만 확실히 가림.

### 3.3 전송·입력·노출
- 외부 호출 **HTTPS** 강제(base-url https).
- 컨트롤러 입력 **Bean Validation**(좌표/반경/페이지 범위). 위반 시 `VALIDATION_ERROR`(BR-2).
- **접근 통제(U1-a)**: SecurityConfig **permitAll 껍데기**(현재 🔒 없음). deny-by-default 전환은 U1-b.
- **에러 노출 최소화**: 공통 실패 응답으로 감싸 내부/외부 원문 미노출.

---

## 4. 동시성·격리 패턴 (Concurrency & Bulkhead) — D6=A

- 상세 조합(U3)의 **병렬 다중 호출**(detailCommon/Intro/Info/Image)을 위한 **전용 스레드풀 Bean** 정의.
  - `ThreadPoolTaskExecutor` — 고정 코어/최대 크기, 이름 접두사 `tourapi-`, 유한 큐, 포화 정책 명시(CallerRuns 등).
  - **서블릿 요청 스레드와 격리**(벌크헤드): 외부 호출 지연이 톰캣 워커를 고갈시키지 않음.
- U1-a는 **Executor Bean + 설정만 마련**, 실제 `CompletableFuture` 조합(PlaceDetailAssembler)은 U3.
- `@TimeLimiter`(Resilience4j) 사용을 위해서도 별도 실행기 필요 → 이 전용 풀과 정합.

---

## 5. 관측성 패턴 (Observability) — N6

- **구조화 로깅**(SLF4J/Logback): 외부 호출 시작/성공/실패, 소요시간, 서킷 상태전이.
- 이벤트 로깅 항목: 외부 API 실패, **캐시 히트/미스**, **미매핑 카테고리 WARN**(BR-4.4), **좌표 파싱 제외 카운트**(BR-3.4).
- Resilience4j 이벤트(서킷 open/half-open/close, 재시도) 로깅 → 장애 가시성.
- 정량 성능 목표는 미설정(N4=C), 로깅으로 **관측만**.

---

## 6. 패턴 ↔ NFR 추적 매트릭스

| NFR 요구 | 적용 패턴 | 결정 |
|---|---|---|
| 가용성(외부 격리) | Resilience4j 어노테이션, 엔드포인트별 서킷 | D1=A, D2=B |
| 성능/부하완화 | Redis @Cacheable, TTL 30분+, 병렬 벌크헤드 | D3=A, D6=A |
| 신뢰성(실패 명확화) | 실패 정규화 폴백, stale 미사용 | D4=A |
| 보안(시크릿/로그) | env 주입, 호출지점 마스킹, permitAll 한정 | D5=A, N3 |
| 관측성 | 구조화 로깅 + Resilience/캐시 이벤트 | N6 |
