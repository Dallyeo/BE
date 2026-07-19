# Logical Components — U1-a 공개 기반 (NFR Design)

> NFR 패턴(`nfr-design-patterns.md`)을 뒷받침하는 **논리 컴포넌트/설정 배선**. U1-a는 "기반"이므로 대부분 **설정·Bean 스캐폴딩**을 마련하고, 실제 사용(엔드포인트 배선, 병렬 조합)은 U2/U3에서 부착.
> 도메인 타입은 `functional-design/domain-entities.md` 참조.

---

## 1. 컴포넌트 목록

| 논리 컴포넌트 | 유형 | 역할 | NFR 근거 | U1-a 범위 |
|---|---|---|---|---|
| `TourApiClient` | 외부 연동 클라이언트 | TourAPI 호출(RestClient), 회복성 어노테이션 부착, 응답 파싱 | 가용성/성능 | 인터페이스+기반 호출 1~2개 골격 |
| `TourApiProperties` | @ConfigurationProperties | base-url/serviceKey/타임아웃/TTL 바인딩 | 보안/설정외부화 | ✅ 완성 |
| `ResilienceConfig`(설정) | application.properties | 엔드포인트별 서킷/재시도/타임리미터 인스턴스 | 가용성 D1/D2 | ✅ default+명명규칙 |
| `CacheConfig` | @Configuration | @EnableCaching + RedisCacheManager, TTL/직렬화 | 성능 D3 | ✅ 완성 |
| `TourApiExecutor` | Bean(ThreadPoolTaskExecutor) | 병렬 외부호출 전용 스레드풀(벌크헤드) | 동시성 D6 | ✅ Bean 정의 |
| `LogMaskingUtil` | 공통 유틸 | serviceKey/민감값 마스킹 후 로깅 | 보안 D5 | ✅ 완성 |
| `GlobalExceptionHandler` | @RestControllerAdvice | 예외 → 공통 실패 응답(`ApiResponse`) 정규화 | 신뢰성/보안 | ✅ (FD 산출물 연계) |
| `SecurityConfig` | @Configuration | permitAll 껍데기(현재 🔒 없음) | 보안(U1-b 전환) | ✅ 껍데기 |

---

## 2. 컴포넌트별 상세

### 2.1 TourApiClient
- **의존**: `RestClient`(HTTPS base-url), `TourApiProperties`, `LogMaskingUtil`, `TourApiExecutor`(병렬 시).
- **회복성 배선(D1/D2)**: 각 오퍼레이션 메서드에 `@Retry(name="tourApiXxx")` + `@CircuitBreaker(name="tourApiXxx", fallbackMethod=...)` + `@TimeLimiter(name="tourApiXxx")`.
  - 인스턴스명은 엔드포인트별(§패턴 1.2 표).
- **폴백**: 각 메서드에 fallbackMethod → 예외를 `EXTERNAL_API_ERROR`로 변환.
- **로깅**: 호출 URL 로깅 전 `LogMaskingUtil.maskServiceKey(url)`.
- **U1-a 범위**: 인터페이스 + RestClient 구성 + 1개 대표 호출(예: areaBasedList) 골격으로 계약 검증. 나머지 엔드포인트는 U2/U3.

### 2.2 TourApiProperties (@ConfigurationProperties "tourapi")
```
tourapi.base-url=https://apis.data.go.kr/B551011/KorService2
tourapi.service-key=${TOURAPI_SERVICE_KEY}
tourapi.connect-timeout=2s
tourapi.read-timeout=3s
tourapi.cache-ttl=30m
```
- serviceKey는 env 전용(코드/VCS 금지, N3).

### 2.3 Resilience4j 설정 (application.properties, D1/D2)
- **default config**(모든 인스턴스 상속):
```
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.configs.default.sliding-window-type=COUNT_BASED
resilience4j.retry.configs.default.max-attempts=2        # 최초1 + 재시도1
resilience4j.retry.configs.default.wait-duration=... (지수 백오프)
resilience4j.retry.configs.default.retry-exceptions=타임아웃/5xx 계열
resilience4j.retry.configs.default.ignore-exceptions=4xx 계열
resilience4j.timelimiter.configs.default.timeout-duration=3s
```
- **엔드포인트별 인스턴스**: `...instances.tourApiAreaBased.base-config=default` 형태로 default 상속, 필요 시만 오버라이드.
- U1-a: default + 명명 규칙만 확정. 인스턴스 상세는 U2/U3에서 추가.

### 2.4 CacheConfig (D3)
- `@EnableCaching`, `RedisCacheManager` Bean.
- `RedisCacheConfiguration`: `entryTtl(30m)`, key `StringRedisSerializer`, value `GenericJackson2JsonRedisSerializer`.
- 캐시 이름/키 규칙: §패턴 2.1. 캐시 오류(Redis 다운) 시 조회는 원본 호출로 폴백(캐시는 best-effort) — 앱 장애로 전파 금지.

### 2.5 TourApiExecutor (D6, 벌크헤드)
- `ThreadPoolTaskExecutor` Bean: core/max 고정(예: core 8/max 16, 큐 유한), thread-name-prefix `tourapi-`, 포화정책 CallerRunsPolicy(또는 거부+로깅).
- `@TimeLimiter` 및 U3 `CompletableFuture` 병렬 조합의 실행기로 사용.
- 서블릿 워커와 격리 → 외부 지연 전파 차단.

### 2.6 LogMaskingUtil (D5)
- `maskServiceKey(String url|params)` → serviceKey 값 `***` 치환.
- 확장 지점: 토큰/쿠키 마스킹(U1-b). 외부호출 로깅 지점에서만 호출(전역 컨버터 미사용).

### 2.7 SecurityConfig (U1-a 껍데기)
- 모든 요청 `permitAll`(현재 인증 대상 엔드포인트 없음). CSRF/세션 stateless 기본.
- deny-by-default·화이트리스트 정교화·JWT 필터는 **U1-b**로 이관(주석/TODO로 전환 지점 명시).

---

## 3. 컴포넌트 상호작용 (외부 조회 흐름)

```
Controller
  └─ (Bean Validation) → Service
       └─ @Cacheable(Redis)  ── HIT ──▶ 캐시 값 반환
            │ MISS
            ▼
         TourApiClient.operation()
            ├─ @Retry → @CircuitBreaker(endpoint별) → @TimeLimiter
            ├─ RestClient(HTTPS)  ── 성공 ─▶ 파싱 → 캐시에 저장 → 반환
            └─ 실패(서킷open/재시도소진/타임아웃)
                 └─ fallback → EXTERNAL_API_ERROR(502)  [stale 미사용, D4]
      로깅: LogMaskingUtil로 serviceKey 마스킹 + 캐시히트/실패/서킷이벤트(구조화)
```

- 병렬 상세 조합(U3): Service가 `TourApiExecutor`로 detailCommon/Intro/Info/Image를 `CompletableFuture`로 동시 호출 → 조립. 각 호출은 자기 엔드포인트 서킷 적용.

---

## 4. U1-a 산출 경계 요약
- **U1-a에서 실제 구현**: Properties/CacheConfig/Executor/LogMaskingUtil/SecurityConfig 껍데기 + Resilience default 설정 + TourApiClient 골격(대표 1콜).
- **U2/U3로 이관**: 엔드포인트별 서킷 인스턴스 배선, 나머지 오퍼레이션, 병렬 조합(PlaceDetailAssembler).
- **U1-b로 이관**: deny-by-default 전환, JWT 필터, 토큰 마스킹 확장.
