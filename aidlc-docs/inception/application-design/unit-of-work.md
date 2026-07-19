# Unit of Work — Dallyeo

> **배포 형태**: 모놀리식 단일 배포(Spring Boot). 유닛 = **한 앱 안의 논리 모듈 + 빌드 순서**(독립 배포 서비스 아님).
> **진행 방식**: 순차 수직완성 — 유닛별 설계→코드→테스트 완료 후 다음 유닛(U2=A).
> **패키지 전략**: 도메인 우선 `com.ppip.dallyeo.<domain>` (controller/service/repository).

범례: 🌐 공개 · 🔒 인증 필요

---

## 코드 조직 전략
```
com.ppip.dallyeo
├─ common/           # 응답 래퍼, 예외, 유틸
├─ config/           # Security, Cache(Redis), Resilience4j, Properties
├─ external/
│  ├─ tourapi/       # TourApiClient(Facade) + DTO + 매핑
│  └─ oauth/         # Kakao/Apple OAuthClient (U1-b/U4)
├─ region/  course/  place/  badge/     # 공개 도메인
└─ auth/  user/  run/                    # 인증/개인 도메인
```
- 각 도메인 패키지 내부: `controller` / `service` / `repository` / `dto` / `entity`.
- 초기 데이터 적재기는 **각 도메인 유닛에 포함**(U3=A): `CourseDataLoader`(course), `BadgeCsvLoader`(badge).

---

## 유닛 정의

### Unit 1. 공통·기반 (Foundation) — **2단계 분리**

기반 유닛을 **U1-a(지금) / U1-b(U4 착수 시)** 로 나눔.
근거: U2·U3이 전부 공개 API라 그 구현에는 인증이 불필요 → 인증/JWT/소셜검증은 실제 필요 시점(U4)에 구현.

#### U1-a — 공개 기반 (지금 구현)
| 포함 | 컴포넌트 |
|---|---|
| 공통 응답 래퍼 | `ApiResponse<T>`, `ApiError` |
| 전역 예외 처리 | `GlobalExceptionHandler` |
| TourAPI 연동 + 설정 | `TourApiClient`(Facade), `TourApiProperties`, `ResilienceConfig`(Resilience4j), `CacheConfig`(Redis) |
| Security 껍데기 | `SecurityConfig` — **전체 permitAll**(공개 API 우선 오픈, 인증 로직 없음) |
| 설정 교정 | 로거 설정 교정, RegionCodeMapper |

> **주의**: U1-a의 `SecurityConfig`는 전체 permitAll 껍데기. 아직 🔒 엔드포인트(users/runs)가 없으므로 안전. U1-b에서 deny-by-default + 화이트리스트로 정교화.

#### U1-b — 인증 기반 (U4 착수 시 구현)
| 포함 | 컴포넌트 |
|---|---|
| 토큰 | `JwtProvider`(발급/검증), JWT 라이브러리 추가(build.gradle) |
| 필터/보안 | `JwtAuthenticationFilter`, `SecurityConfig` **정교화**(deny-by-default + 공개 화이트리스트) |
| 소셜 검증 | `OAuthClient`(Kakao/Apple) |

---

### Unit 2. 지역·코스조회 🌐 (U1-a 이후)
- **도메인**: region, course(조회)
- **컴포넌트**: RegionController/Service/RegionCatalog, CourseController/CourseQueryService/CourseRepository, **CourseDataLoader**(courses.json 시드)
- **의존**: U1-a(공통/응답/예외). 인증 불필요.

### Unit 3. 장소·배지 🌐 (U1-a 이후)
- **도메인**: place, badge
- **컴포넌트**: PlaceController/Service/DetailAssembler/Mapper, BadgeService/Repository/**BadgeCsvLoader**/AddressNormalizer
- **의존**: U1-a(**TourApiClient**, 캐시/회복성). 인증 불필요.

### Unit 4. 인증·사용자 🔒 (U1-b 포함 착수)
- **도메인**: auth, user
- **컴포넌트**: AuthController/Service/RefreshTokenStore, UserController/Service/Repository
- **선행**: **U1-b(JWT/필터/OAuthClient)를 이 시점에 함께 구현**.
- **의존**: U1-a + U1-b.

### Unit 5. 러닝기록·코스생성 🔒 (U4 이후)
- **도메인**: run, course(생성)
- **컴포넌트**: RunController/Service/Repository, CourseCommandService
- **의존**: U4(토큰 체계) — 소유권 검증 전제.

---

## 빌드 순서
```
U1-a (공개 기반) → U2 (지역·코스조회🌐) → U3 (장소·배지🌐)
                                            → U4 (인증·사용자🔒, U1-b 함께) → U5 (러닝·코스생성🔒)
```
- **조기 전달**: U1-a→U2→U3 완료 시 공개 API(지역/코스/장소) 프론트 선제공 가능.
- **인증 도입**: U4에서 U1-b(JWT/보안 정교화)와 함께.

## 범위 밖(보류 백로그)
완주율·통계·공유결과, 유사/최근검색어, 프로필사진, 약관, 업적, 편의시설(화장실) 좌표 — 후순위.
