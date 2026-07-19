# Components — Dallyeo

> 고수준 컴포넌트 식별 및 책임. 상세 비즈니스 로직/데이터 모델은 Functional Design(단위별)에서.
> 기본 패키지: `com.ppip.dallyeo` · 패키지 전략: **도메인 우선**(D1) · 각 도메인 = controller/service/repository

범례: 🌐 공개 · 🔒 인증 필요 · (F) 공통·기반

---

## Unit 1. 공통·기반 (Foundation) — `com.ppip.dallyeo.common`, `.config`, `.external.tourapi`

| 컴포넌트 | 책임 | 인터페이스(개략) |
|---|---|---|
| **ApiResponse / ApiError** | 공통 성공/실패 응답 래퍼 | `ApiResponse<T>`, `ApiError(code,message)` |
| **GlobalExceptionHandler** | 전역 예외 → 공통 실패 응답 + HTTP status 매핑 | `@RestControllerAdvice` |
| **SecurityConfig** | deny-by-default + 공개 화이트리스트, 필터체인 구성 | `SecurityFilterChain` |
| **JwtAuthenticationFilter** | 요청 토큰 검증 → 인증 컨텍스트 주입 | `OncePerRequestFilter` |
| **JwtProvider** | Access/Refresh 토큰 발급·검증 | `issueAccess/issueRefresh/validate/parse` |
| **TourApiClient** (F) | 외부 TourAPI 단일 Facade(오퍼레이션별 메서드) | 아래 §component-methods |
| **TourApiProperties** | TourAPI 키/베이스URL/타임아웃 설정 바인딩 | `@ConfigurationProperties` |
| **ResilienceConfig** | Resilience4j 타임아웃/재시도/서킷브레이커 | 설정 빈 |
| **CacheConfig** | Redis 단기 캐시 설정 | `CacheManager` |
| **RegionCodeMapper** | Region enum ↔ 법정동코드(전북52/군산130/전주110) | `toLDongCode(Region)` |

---

## Unit 2. 지역·코스조회 🌐

### 지역 — `com.ppip.dallyeo.region`
| 컴포넌트 | 책임 |
|---|---|
| **RegionController** 🌐 | `GET /regions` |
| **RegionService** | 지원 지역 목록 제공(군산/전주) |
| **RegionCatalog** | 지역 정의(코드/이름/법정동 매핑) 보관 |

### 코스(조회) — `com.ppip.dallyeo.course`
| 컴포넌트 | 책임 |
|---|---|
| **CourseController** 🌐(조회) | `GET /courses`, `GET /courses/{id}` |
| **CourseQueryService** | 코스 목록/상세 조회(요약 vs 상세) |
| **CourseRepository** | 코스 영속성(JPA) |
| **CourseDataLoader** | `courses.json` 시드 적재 |

---

## Unit 3. 장소·배지 🌐

### 장소 — `com.ppip.dallyeo.place`
| 컴포넌트 | 책임 |
|---|---|
| **PlaceController** 🌐 | `GET /places/search`, `/places`, `/places/nearby`, `/places/{id}` |
| **PlaceService** | TourApiClient 오케스트레이션(검색/목록/반경) |
| **PlaceDetailAssembler** | 상세 4콜 **병렬 조합**(D3/D4) |
| **PlaceMapper** | TourAPI DTO → Place DTO 변환(좌표 파싱·빈값 null·카테고리 매핑, 관광지/음식점 분기) |

### 배지 — `com.ppip.dallyeo.badge`
| 컴포넌트 | 책임 |
|---|---|
| **BadgeService** | 장소에 배지 부여(정규화 키 매칭) |
| **BadgeRepository** | 배지 테이블 영속성(JPA) |
| **BadgeCsvLoader** | 모범음식점/착한가격업소 CSV 적재(요식업만, EUC-KR 변환) |
| **AddressNormalizer** | 업소명+주소 정규화(공백/괄호/특수문자 제거) |

---

## Unit 4. 인증·사용자 🔒

### 인증 — `com.ppip.dallyeo.auth`
| 컴포넌트 | 책임 |
|---|---|
| **AuthController** | `POST /auth/login/{provider}` 🌐, `/auth/refresh` 🌐, `/auth/logout` 🔒 |
| **AuthService** | 로그인/가입·토큰 발급/갱신·로그아웃 오케스트레이션 |
| **OAuthClient** (Kakao/Apple 구현) | 소셜 인가 검증·사용자정보 조회(D8 백엔드 검증) |
| **RefreshTokenStore** | Refresh Token Redis 저장/조회/삭제 |

### 사용자 — `com.ppip.dallyeo.user`
| 컴포넌트 | 책임 |
|---|---|
| **UserController** 🔒 | `GET /users/me`, `PATCH /users/me`, `DELETE /users/me` |
| **UserService** | 프로필 조회/수정(온보딩 신체정보)·계정 삭제 |
| **UserRepository** | 사용자 영속성(JPA) |

---

## Unit 5. 러닝기록·코스생성 🔒

### 러닝기록 — `com.ppip.dallyeo.run`
| 컴포넌트 | 책임 |
|---|---|
| **RunController** 🔒 | `POST /runs`, `GET /runs`, `GET /runs/{id}` |
| **RunService** | 기록 저장/조회/상세 + **소유권 검증** |
| **RunRepository** | 러닝기록 영속성(JPA) |

### 코스(생성) — `com.ppip.dallyeo.course`
| 컴포넌트 | 책임 |
|---|---|
| **CourseCommandService** 🔒 | 사용자 코스 생성(Tmap JSON 저장, `POST /courses`) |

> 참고: 완주율·통계·공유결과·유사/최근검색어·프로필사진·업적·편의시설(화장실) 좌표는 **보류 백로그** — 이번 설계 범위 밖.
