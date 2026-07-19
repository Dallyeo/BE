# Application Design (통합) — Dallyeo

> 애플리케이션 설계 통합 문서. 세부는 `components.md` / `component-methods.md` / `services.md` / `component-dependency.md` 참조.
> 상세 비즈니스 로직·데이터 모델은 Functional Design(단위별, CONSTRUCTION)에서 확정.

## 1. 설계 결정 요약 (승인됨)
| # | 결정 |
|---|---|
| D1 | 패키지 **도메인 우선** (`com.ppip.dallyeo.<domain>` 안에 controller/service/repository) |
| D2 | TourAPI **단일 Facade**(`TourApiClient`) |
| D3 | 장소 상세 **백엔드 병렬 조합** |
| D4 | 병렬 수단 **RestClient + CompletableFuture** |
| D5 | 배지 **DB 테이블 적재 + 정규화 키 조인** |
| D6 | TourAPI **Redis 단기 캐시** |
| D7 | 외부 회복성 **Resilience4j** |
| D8 | 소셜 로그인 **백엔드 검증** |
| D9 | 공통·기반에 **전부 포함** |

## 2. 아키텍처 개요
- **모놀리식 Spring Boot 4.0.6 / Java 17**, 레이어드(Controller→Service→Repository) + 도메인 패키지.
- **영속성**: MySQL(JPA) — User/Course/Run/Badge, Region은 정적. Redis — Refresh Token + TourAPI 캐시.
- **외부 연동**: TourAPI(실시간 프록시, Facade+Resilience4j+캐시), Kakao/Apple(소셜 검증).
- **보안**: deny-by-default + 공개 화이트리스트, JWT 필터(Access 24h/Refresh 7d).

## 3. 도메인·컴포넌트 (8도메인 / 5유닛)
| 유닛 | 도메인 | 대표 컴포넌트 |
|---|---|---|
| 1 | 공통·기반 | ApiResponse, GlobalExceptionHandler, SecurityConfig, JwtProvider, **TourApiClient**, Resilience/Cache 설정, RegionCodeMapper |
| 2 🌐 | 지역, 코스(조회) | RegionController/Service, CourseController/CourseQueryService |
| 3 🌐 | 장소, 배지 | PlaceController/Service, PlaceDetailAssembler, PlaceMapper, BadgeService/CsvLoader |
| 4 🔒 | 인증, 사용자 | AuthController/Service, OAuthClient, UserController/Service |
| 5 🔒 | 러닝기록, 코스(생성) | RunController/Service, CourseCommandService |

## 4. 핵심 오케스트레이션
- **장소 상세**: PlaceService → PlaceDetailAssembler가 detailCommon/Intro/Info/Image **병렬 호출** → PlaceMapper 병합 → BadgeService 배지 → 응답.
- **소셜 로그인**: AuthService → OAuthClient 검증 → User upsert → JwtProvider 발급 → RefreshTokenStore(Redis).
- **배지**: 장소 목록/상세에 BadgeService가 정규화 키로 일괄 부여(요식업만, N+1 방지 batch).

## 5. 횡단 관심사(Cross-cutting)
- 공통 응답 래퍼 / 전역 예외 처리 (전 도메인).
- 인증 필터(공개/보호 분기) — `auth-classification.md` 기준.
- 외부 연동 회복성(Resilience4j) + Redis 캐시 — TourApiClient.
- 좌표/빈값 정규화(PlaceMapper), 주소 정규화(AddressNormalizer).

## 6. 빌드 순서 (공개 우선)
1) 공통·기반 → 2) 지역·코스조회 🌐 → 3) 장소·배지 🌐 → 4) 인증·사용자 🔒 → 5) 러닝기록·코스생성 🔒
- Unit 2·3 완료 시 공개 API 선제공 가능. Unit 5는 토큰 체계(Unit 4) 이후.

## 7. 범위 밖(보류 백로그)
완주율·통계·공유결과, 유사/최근검색어, 프로필사진, 약관 정적콘텐츠, 업적, 편의시설(화장실) 좌표·지오코딩 — 회의/후순위.

## 8. 검증(완전성·일관성)
- ✅ 21개 스토리 → 컴포넌트/메서드 매핑 커버.
- ✅ 🔒/🌐 분류가 컨트롤러/필터 설계에 반영.
- ✅ 외부 연동은 Facade 경유로 일관.
- ✅ 유닛 의존 순서 무순환(Unit1 기반 → 2/3 → 4 → 5).
