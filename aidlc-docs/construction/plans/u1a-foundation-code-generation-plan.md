# Code Generation Plan — U1-a 공개 기반

> **이 문서는 U1-a Code Generation의 단일 진실 소스(single source of truth)**입니다. Part 2에서는 이 단계 순서를 정확히 따르고, 각 단계 완료 시 즉시 `[x]` 표기합니다.
> 프로젝트: Brownfield / Spring Boot 4.0.6 / Java 17 / Gradle. **Base package**: `com.ppip.dallyeo`. 앱 코드 위치: `src/main/java/...`, 테스트: `src/test/java/...` (절대 aidlc-docs/ 아래 아님).

---

## 유닛 컨텍스트

### 구현 스토리 (story map)
- **US-COMMON-1** 공통 응답 래퍼 (`ApiResponse`, `ApiError`, `ErrorDetail`)
- **US-COMMON-2** 전역 예외 처리 (`GlobalExceptionHandler`)
- **US-COMMON-4** TourAPI 연동 클라이언트 (Facade + Resilience4j + Redis 캐시)
- **US-COMMON-3(부분)** 설정 정리 — **로거 교정**만 U1-a (JWT 라이브러리는 U1-b)
- **US-AUTH-4(부분)** Security **permitAll 껍데기**만 U1-a (deny-by-default는 U1-b)

### 의존/경계
- 선행 의존: 없음(기반 유닛). 후속 U2/U3/U4가 이 유닛에 의존.
- **영속 엔티티 없음** → Repository 레이어/DB 마이그레이션 **N/A**. 프론트엔드 **N/A**.
- **U1-a 골격 원칙**: TourApiClient는 **대표 1콜(areaBasedList2)** 로 계약·정규화·캐시·회복성을 검증하는 골격까지. 나머지 엔드포인트(location/keyword/detail*) 배선은 U2/U3.
- **U1-b 이관**: JWT 라이브러리/필터, deny-by-default 전환, 소셜 검증.

### 설계 근거
- functional-design: `business-logic-model.md`, `business-rules.md`(BR-1~7), `domain-entities.md`
- nfr-design: `nfr-design-patterns.md`(D1 어노테이션, D2 엔드포인트별 서킷, D3 @Cacheable, D4 stale미사용, D5 호출지점 마스킹, D6 전용풀), `logical-components.md`
- infrastructure-design: 단일 EC2 코로케이션, env 시크릿

### 목표 패키지 구조 (`src/main/java/com/ppip/dallyeo/`)
```
common/
  response/   ApiResponse, ApiError, ErrorDetail
  exception/  ErrorCode, BusinessException, ExternalApiException, GlobalExceptionHandler
  util/       LogMaskingUtil
config/       SecurityConfig, CacheConfig, AsyncConfig(TourApiExecutor), RestClientConfig
external/tourapi/
  TourApiProperties, TourApiClient, TourApiNormalizer
  dto/        TourItem, TourCommon, TourIntro, TourInfo, TourImage
domain/
  category/   CategoryType, PlaceCategory, CategoryMapper
  region/     Region, LDongCode, RegionCatalog, RegionCodeMapper
```

---

## 실행 단계 (Part 2에서 순서대로)

### Step 1 — 빌드/의존성 설정 (build.gradle)  [US-COMMON-3, US-COMMON-4]
- [x] `build.gradle`에 추가: **Resilience4j**(spring-boot3 스타터, Boot 4 호환 버전 확인·고정), **Jackson**(web에 포함, 명시 불필요), 테스트용 **WireMock**.
- [x] jjwt는 **U1-b로 유보**(주석 TODO만) — U1-a는 인증 로직 없음.
- [x] Boot 4.0.x BOM과의 버전 정합 확인 노트 반영.

### Step 2 — 설정 파일 교정/추가 (application.properties)  [US-COMMON-3, US-COMMON-4]
- [x] **로거 교정(US-COMMON-3)**: 잘못된 `logging.level.com.celdog` → `logging.level.com.ppip.dallyeo`.
- [x] `tourapi.*` 추가: base-url, service-key(`${TOURAPI_SERVICE_KEY}`), connect-timeout 2s, read-timeout 3s, cache-ttl 30m.
- [x] `resilience4j.*` default config + 엔드포인트별 인스턴스 명명(D2): circuitbreaker(50%/10s), retry(max 2/백오프/5xx·timeout 대상), timelimiter(3s).
- [x] 캐시/구조화 로깅 관련 프로퍼티. (시크릿 실제 값은 미기입 — env/secret 프로파일)

### Step 3 — 공통 응답 래퍼 생성  [US-COMMON-1]
- [x] `common/response/ApiResponse<T>` — success/data/error + 정적 팩토리 `success(T)`, `success()`, `failure(ApiError)`.
- [x] `common/response/ApiError` — code/message/details.
- [x] `common/response/ErrorDetail` — field/message.

### Step 4 — 공통 응답 단위 테스트  [US-COMMON-1]
- [x] `ApiResponseTest` — 성공/실패 팩토리, 필드 매핑 검증.

### Step 5 — 에러코드/예외 타입 생성  [US-COMMON-2] (BR-1)
- [x] `common/exception/ErrorCode` enum — 코드 문자열 + HTTP status (VALIDATION_ERROR/BAD_REQUEST/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/CONFLICT/EXTERNAL_API_ERROR/INTERNAL_ERROR).
- [x] `common/exception/BusinessException`(code+message), `common/exception/ExternalApiException`(→ EXTERNAL_API_ERROR).

### Step 6 — 전역 예외 처리기 생성  [US-COMMON-2] (BR-1/BR-2)
- [x] `common/exception/GlobalExceptionHandler`(@RestControllerAdvice) — business-logic-model §2 매핑표대로. Validation 실패 → 필드별 details(BR-2). 내부/외부 원문 미노출(보안).

### Step 7 — 예외 처리 단위 테스트  [US-COMMON-2]
- [x] `GlobalExceptionHandlerTest`(@WebMvcTest 슬라이스 또는 단위) — 검증실패/BAD_REQUEST/EXTERNAL_API_ERROR/INTERNAL_ERROR 응답 형태.

### Step 8 — 로그 마스킹 유틸  [US-COMMON-4] (D5)
- [x] `common/util/LogMaskingUtil` — URL/파라미터 내 `serviceKey` 값 마스킹(`***`).
- [x] `LogMaskingUtilTest` — serviceKey 치환/비대상 보존.

### Step 9 — 카테고리 매핑 생성  [US-COMMON-4] (BR-4)
- [x] `domain/category/CategoryType` enum(10종), `PlaceCategory`(type+rawContentTypeId) VO.
- [x] `domain/category/CategoryMapper` — contentTypeId(+lclsSystm2 FD05→CAFE) → CategoryType, 미매핑 ETC 폴백 + rawContentTypeId 보존 + **WARN 로그**(BR-4.4).

### Step 10 — 카테고리 매핑 단위 테스트  [US-COMMON-4]
- [x] `CategoryMapperTest` — 매핑표 전건 + 39+FD05=CAFE + 미매핑 ETC 폴백/rawContentTypeId 보존.

### Step 11 — 지역 매핑 생성  [US-COMMON-4] (BR-5)
- [x] `domain/region/Region` enum(GUNSAN, JEONJU), `LDongCode`(regnCd/signguCd) VO, `RegionCatalog`(상수), `RegionCodeMapper`(Region→LDongCode, 미지원 → BAD_REQUEST).

### Step 12 — 지역 매핑 단위 테스트  [US-COMMON-4]
- [x] `RegionCodeMapperTest` — GUNSAN(52,130)/JEONJU(52,110) + 미지원 예외.

### Step 13 — TourAPI 설정/DTO 생성  [US-COMMON-4]
- [x] `external/tourapi/TourApiProperties`(@ConfigurationProperties "tourapi"): baseUrl/serviceKey/connectTimeout/readTimeout/cacheTtl.
- [x] `external/tourapi/dto/`: TourItem, TourCommon, TourIntro, TourInfo, TourImage (domain-entities §2 필드).

### Step 14 — TourAPI 정규화기 생성  [US-COMMON-4] (BR-3)
- [x] `external/tourapi/TourApiNormalizer` — 빈문자열→null, mapx/mapy/dist 파싱, **엔드포인트별 좌표 정책**(location 필수·제외 / area·keyword null 유지), 제외/실패 **카운트 로그**(BR-3.4).
- [x] `TourApiNormalizerTest` — 정규화/좌표정책/제외 카운트.

### Step 15 — 설정 Bean 생성 (config)  [US-COMMON-4, US-AUTH-4]
- [x] `config/RestClientConfig` — RestClient(HTTPS base-url, 타임아웃) 빈.
- [x] `config/CacheConfig` — @EnableCaching + RedisCacheManager(TTL 30m, key/value 직렬화)(D3).
- [x] `config/AsyncConfig` — `TourApiExecutor` ThreadPoolTaskExecutor 빈(벌크헤드, D6).
- [x] `config/SecurityConfig` — **전체 permitAll 껍데기**(US-AUTH-4 부분), stateless, deny-by-default는 U1-b TODO 주석.

### Step 16 — TourApiClient(Facade) 생성  [US-COMMON-4] (BR-6/BR-7)
- [x] `external/tourapi/TourApiClient` — 대표 오퍼레이션 `areaBasedList2` 골격: RestClient 호출 + `@Cacheable`(키 전략) + Resilience4j 어노테이션(`@Retry`/`@CircuitBreaker(name=tourApiAreaBased, fallback)`/`@TimeLimiter`) + LogMaskingUtil 로깅 + Normalizer 적용 + fallback→ExternalApiException.
- [x] 나머지 엔드포인트는 확장 지점(주석 TODO: U2/U3).

### Step 17 — TourApiClient 단위 테스트 (WireMock)  [US-COMMON-4]
- [x] `TourApiClientTest` — WireMock으로 정상 응답 파싱/정규화, 5xx→재시도→EXTERNAL_API_ERROR 폴백, (가능 시 캐시 히트) 검증.

### Step 18 — 코드 요약 문서 생성 (aidlc-docs)
- [x] `aidlc-docs/construction/u1a-foundation/code/code-summary.md` — 생성/수정 파일 목록, 패키지 구조, 스토리 추적, U2/U3/U1-b 확장 지점.

### Step 19 — 배포 아티팩트(경량)
- [x] `.env.example`(시크릿 키 목록, 값 없음) + `.gitignore`에 `.env`/`logs/` 확인·보강. (systemd 유닛 샘플은 deployment-architecture.md 참조로 대체, 파일 생성은 선택.)

---

## 산출물 위치 요약
- **앱 코드/빌드/설정**: `src/main/java/com/ppip/dallyeo/...`, `src/test/java/...`, `build.gradle`, `src/main/resources/application.properties`, `.env.example`, `.gitignore`
- **문서(마크다운)**: `aidlc-docs/construction/u1a-foundation/code/`

## 범위 밖 (본 유닛 N/A / 이관)
- Repository/DB 마이그레이션 — 영속 엔티티 없음(N/A)
- 프론트엔드 — 백엔드 유닛(N/A)
- JWT/인증 필터/deny-by-default/소셜검증 — **U1-b**
- location/keyword/detail* 엔드포인트 배선, 병렬 상세 조합 — **U2/U3**
