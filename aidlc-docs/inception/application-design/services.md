# Services & Orchestration — Dallyeo

> 서비스 계층의 책임 경계와 오케스트레이션 패턴. 도메인 서비스는 자기 도메인 리포지토리 + 필요한 기반 컴포넌트만 협력.

---

## 오케스트레이션 원칙
- **컨트롤러**: 요청 검증 + DTO 변환 + 서비스 위임 (얇게). 응답은 `ApiResponse<T>`.
- **서비스**: 도메인 비즈니스 흐름. 트랜잭션 경계(🔒 쓰기 도메인).
- **외부 연동**: 반드시 `TourApiClient`(Facade) / `OAuthClient` 를 통해서만. 도메인 서비스가 외부 HTTP 직접 호출 금지.
- **읽기/쓰기 분리**: 코스는 `CourseQueryService`(🌐) / `CourseCommandService`(🔒) 로 분리.

---

## 서비스별 상세

### RegionService 🌐
- **책임**: 지원 지역 목록 반환.
- **협력**: RegionCatalog(정적 정의).
- **오케스트레이션**: 없음(단순 조회).

### CourseQueryService 🌐
- **책임**: 코스 목록(요약)/상세(경로·경유지) 조회.
- **협력**: CourseRepository.
- **비고**: 목록은 경로 좌표 제외, 상세는 polyline/cumulativeMeters/waypointAnchors 포함.

### PlaceService 🌐 (핵심 오케스트레이터)
- **책임**: 장소 검색/목록/반경/상세를 TourAPI로 조합.
- **협력**: `TourApiClient`, `PlaceMapper`, `PlaceDetailAssembler`, `BadgeService`.
- **오케스트레이션 흐름**:
  - 목록/검색/반경 → TourApiClient 목록 오퍼레이션 1콜 → PlaceMapper 변환 → BadgeService로 배지 일괄 부여(요식업).
  - 상세 → `PlaceDetailAssembler.assemble()`가 detailCommon/Intro/Info/Image **병렬(CompletableFuture)** 호출 → PlaceMapper 병합 → BadgeService 배지.
- **회복성/캐시**: TourApiClient 레벨에서 Resilience4j + Redis 캐시 적용.

### BadgeService 🌐
- **책임**: 정규화 키(업소명+주소)로 장소에 배지 부여.
- **협력**: BadgeRepository, AddressNormalizer.
- **비고**: 목록 조회 시 N+1 방지 위해 일괄 조회(batch) 메서드 사용.

### AuthService 🔒/🌐
- **책임**: 소셜 로그인(가입/조회) → 토큰 발급, 갱신, 로그아웃.
- **협력**: `OAuthClient`(검증), `UserRepository`(가입/조회), `JwtProvider`(발급), `RefreshTokenStore`(Redis).
- **오케스트레이션**:
  - login: OAuthClient.verify → 사용자 upsert → JwtProvider 발급 → RefreshTokenStore 저장 → onboardingRequired 판단.
  - refresh: RefreshTokenStore 대조 → 재발급.
  - logout: RefreshTokenStore 삭제.

### UserService 🔒
- **책임**: 내 프로필 조회/수정(온보딩 신체정보), 계정 삭제.
- **협력**: UserRepository.
- **트랜잭션**: 수정/삭제 쓰기 트랜잭션.

### RunService 🔒
- **책임**: 러닝 기록 저장/목록/상세 + **소유권 검증**.
- **협력**: RunRepository, (CourseRepository 참조 optional).
- **소유권**: 조회/상세 시 토큰 userId ≠ 기록 소유자 → 403/404.

### CourseCommandService 🔒
- **책임**: 사용자 코스 생성(Tmap JSON 저장).
- **협력**: CourseRepository.
- **비고**: 백엔드는 Tmap 미호출, 수신 좌표/거리 검증 후 저장.

---

## 데이터 적재(부트스트랩) 서비스
| 로더 | 시점 | 내용 |
|---|---|---|
| **CourseDataLoader** | 앱 기동/마이그레이션 | `courses.json` 10개 코스 시드 |
| **BadgeCsvLoader** | 수동 실행/기동 | 모범음식점(UTF-8)·착한가격업소(EUC-KR→UTF-8, 요식업만) 배지 테이블 적재 |
