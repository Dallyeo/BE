# Code Generation Summary — U1-a 공개 기반

> 생성/수정 산출물 요약. 앱 코드는 `src/...`, 문서는 여기(aidlc-docs). Brownfield: 기존 파일은 in-place 수정.
> **검증**: `./gradlew compileJava` / `compileTestJava` 성공, 순수 단위 테스트(인프라 불필요) 전부 통과. 전체 빌드/통합은 Build & Test 단계.

---

## 1. 생성/수정 파일

### 수정(Modified)
- `build.gradle` — Resilience4j(`resilience4j-spring-boot3:2.2.0`, AOP 전이 포함), 테스트 WireMock 추가. (jjwt는 U1-b 주석 유보)
- `src/main/resources/application.properties` — **로거 교정**(`com.celdog`→`com.ppip.dallyeo`), `tourapi.*`, `resilience4j.*`(엔드포인트별 인스턴스) 추가.
- `.gitignore` — 이미 `.env`/`logs/` 제외 확인(변경 없음).

### 생성(Created) — 앱 코드
| 패키지 | 파일 | 역할 |
|---|---|---|
| common.response | ApiResponse, ApiError, ErrorDetail | 공통 응답 래퍼(US-COMMON-1) |
| common.exception | ErrorCode, BusinessException, ExternalApiException, GlobalExceptionHandler | 에러코드/예외/전역 처리(US-COMMON-2, BR-1/2) |
| common.util | LogMaskingUtil | serviceKey 로그 마스킹(D5) |
| config | RestClientConfig, CacheConfig, AsyncConfig, SecurityConfig | RestClient/Redis캐시/전용풀/permitAll 껍데기 |
| external.tourapi | TourApiProperties, TourApiClient, TourApiNormalizer | TourAPI Facade/설정/정규화(US-COMMON-4, BR-3/6/7) |
| external.tourapi.dto | TourItem, TourCommon, TourIntro, TourInfo, TourImage | 정규화 DTO |
| domain.category | CategoryType, PlaceCategory, CategoryMapper | 카테고리 매핑/폴백(BR-4) |
| domain.region | Region, LDongCode, RegionCatalog, RegionCodeMapper | 지역 매핑(BR-5) |

### 생성(Created) — 테스트
- ApiResponseTest, GlobalExceptionHandlerTest, LogMaskingUtilTest, CategoryMapperTest, RegionCodeMapperTest, TourApiNormalizerTest, TourApiClientTest(WireMock)

### 생성(Created) — 기타
- `.env.example` — 시크릿 키 목록(값 없음): TOURAPI_SERVICE_KEY / DB_* / JWT_SECRET

---

## 2. 스토리 추적 (완료)
| 스토리 | 구현 |
|---|---|
| US-COMMON-1 공통 응답 래퍼 | ApiResponse/ApiError/ErrorDetail ✅ |
| US-COMMON-2 전역 예외 처리 | ErrorCode + GlobalExceptionHandler ✅ |
| US-COMMON-4 TourAPI 연동 클라이언트 | TourApiClient(areaBasedList2 골격)+Normalizer+캐시+회복성 ✅ |
| US-COMMON-3(부분) 설정 정리 | 로거 교정 ✅ (JWT 라이브러리는 U1-b) |
| US-AUTH-4(부분) | SecurityConfig permitAll 껍데기 ✅ (deny-by-default는 U1-b) |

---

## 3. 중요한 기술 메모 (Build & Test/후속 유닛에서 유의)

1. **Spring Boot 4 = Jackson 3**: 패키지가 `com.fasterxml.jackson.databind` → **`tools.jackson.databind`** 로 이동(JsonNode/ObjectMapper). 애노테이션(`@JsonInclude` 등)은 여전히 `com.fasterxml.jackson.annotation`(2.x). Redis 값 직렬화는 **`GenericJacksonJsonRedisSerializer`**(Jackson3용, `builder().build()`) 사용.
2. **Resilience4j 버전**: Spring Boot BOM 미관리 → `2.2.0` 명시 고정. Boot 4.0.x 정식 호환은 Build & Test에서 재확인(현재 컴파일/단위테스트 정상).
3. **@TimeLimiter 미부착(의도적)**: 동기 RestClient 호출의 타임아웃은 RestClient의 connect(2s)/read(3s)로 강제. `@TimeLimiter`는 비동기(CompletableFuture) 경로 필요 → U3 병렬 조합에서 적용 예정. U1-a는 `@Retry`+`@CircuitBreaker`만 부착.
4. **캐시 우선순위**: `@EnableCaching(order = HIGHEST_PRECEDENCE)` — 캐시 HIT 시 Resilience4j/외부호출을 건너뜀(D4: stale 미사용, HIT는 항상 서빙).
5. **serviceKey 인코딩 주의**: RestClient `queryParam` 사용 시 이미 인코딩된 Decoding키/Encoding키 이중 인코딩 가능성 → Build & Test에서 실제 TourAPI로 확인(공공데이터포털 키 형태에 맞춰 조정).
6. **폴백 검증 범위**: WireMock 단위테스트는 프록시 미적용이라 파싱/정규화만 검증. `@CircuitBreaker/@Retry` 폴백→`EXTERNAL_API_ERROR` 정규화는 Spring 컨텍스트(통합) 필요 → Build & Test.

---

## 4. 확장 지점 (다음 유닛)
- **U2/U3**: locationBasedList2/searchKeyword2/detailCommon2·Intro·Info·Image 오퍼레이션을 TourApiClient에 동일 패턴으로 추가(엔드포인트별 서킷 인스턴스 `tourApiLocationBased` 등 properties 추가). 병렬 상세 조합은 `tourApiExecutor` + CompletableFuture(PlaceDetailAssembler).
- **U1-b**: SecurityConfig deny-by-default 전환 + JwtAuthenticationFilter, jjwt 의존성, OAuthClient. LogMaskingUtil에 토큰 마스킹 확장.
