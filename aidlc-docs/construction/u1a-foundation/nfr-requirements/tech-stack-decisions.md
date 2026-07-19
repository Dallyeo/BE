# Tech Stack Decisions — U1-a 공개 기반

> 기존 스택(Spring Boot 4.0.6/Java 17/Gradle/MySQL/Redis/JPA/Security) 위에 U1-a에 필요한 선택.

---

## 확정 기술 선택

| 관심사 | 선택 | 근거 |
|---|---|---|
| **HTTP 클라이언트(외부)** | **Spring RestClient** | MVC 블로킹과 자연스러움, 병렬은 CompletableFuture (D4=A) |
| **병렬 호출** | **CompletableFuture + 스레드풀** | 상세 4콜 병렬(D3/D4) |
| **회복성** | **Resilience4j** | 타임아웃/재시도/서킷브레이커 (D7=A). connect2s/read3s/재시도1/서킷50%·10s (N2=A) |
| **캐시** | **Redis (Spring Data Redis)** | 기존 인프라 재사용, 공유 캐시, TTL 30분+ (D6/F6/BR-6) |
| **JWT** | **jjwt (io.jsonwebtoken)** | 간단·예제 풍부 (N5=A). U1-b에서 사용, build.gradle 추가 |
| **JSON** | **Jackson** | Spring 기본, TourAPI JSON 파싱 |
| **검증** | **Spring Validation (Jakarta)** | 기존 의존성, 컨트롤러 입력 검증 |
| **로깅** | **SLF4J + Logback(구조화)** | 구조화 로깅 + 외부호출/실패/WARN (N6=A) |
| **외부 API 테스트** | **WireMock (또는 MockWebServer)** | TourAPI 목킹 |

## 추가할 의존성 (build.gradle)
- `resilience4j-spring-boot3` (또는 Boot 4 호환 버전) — 회복성
- `io.jsonwebtoken:jjwt-*` — JWT (U1-b 착수 시 실제 사용, 지금 추가 가능)
- 캐시: `spring-boot-starter-data-redis` (이미 존재) + 캐시 추상화 활성(`@EnableCaching`)
- 테스트: WireMock

> **주의(호환성)**: Spring Boot **4.0.x** 기준으로 Resilience4j/jjwt 호환 버전을 Code Generation 시 확인·고정.

## 시크릿/설정 (N3=A)
```
# application.properties (값은 env 주입)
tourapi.base-url=https://apis.data.go.kr/B551011/KorService2
tourapi.service-key=${TOURAPI_SERVICE_KEY}
tourapi.connect-timeout=2s
tourapi.read-timeout=3s
tourapi.cache-ttl=30m
```
- 실제 키: 환경변수 `TOURAPI_SERVICE_KEY` / 로컬 `.env`(git 제외). 코드·VCS 저장 금지.

## 유보 (이번 유닛 밖)
- OAuthClient(Kakao/Apple) 라이브러리·검증 방식 → U1-b/U4에서 확정(애플 JWK 검증은 nimbus 검토 여지).
