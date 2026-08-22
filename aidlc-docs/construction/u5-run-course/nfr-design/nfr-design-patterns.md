# NFR Design Patterns — U5 러닝기록 🔒 (light)

> NFR Requirements(light)를 설계로 구현. 신규 인프라 패턴 없음 — 대부분 U1-a/U4 상속.
> run = 인증 소유자 전용 CRUD(저장/목록/상세). 외부 호출·서킷·캐시·큐 없음.

---

## 1. 보안 패턴

### 1.1 인증 (deny-by-default 상속)
- `/runs/**`는 별도 화이트리스트 없음 → U1-b `SecurityConfig`의 `anyRequest().authenticated()`에 자동 포함.
- 컨트롤러는 `@AuthUser Long userId`로 현재 사용자 획득(`AuthUserArgumentResolver`, U4). 미인증 → EntryPoint 401.

### 1.2 리소스 소유권 (A01 Broken Access Control)
- **목록**: 저장소 쿼리에서 `userId = 현재 사용자` 조건 → 타인 데이터는 결과 집합에 진입 불가(격리).
- **상세**: `findById` 후 `run.userId == 현재 사용자` 확인. 불일치/부재 → **404 NOT_FOUND**(존재 은폐, BR-U5-2). 403 대신 404로 정보 노출 최소화.
- 소유권 판정은 서비스 계층에서 수행(컨트롤러는 위임).

### 1.3 입력 검증 (SECURITY-05)
- 요청 DTO에서 필수/값 검증(polyline 비어있지않음, distance/duration>0, finishedAt>=startedAt, averagePace 존재). 위반 → `BusinessException(BAD_REQUEST)`.
- `from`/`to` 파싱 실패 → 400. 검증은 서비스 진입부(또는 DTO Bean Validation)에서 수행 후 로직 진행.

### 1.4 위치정보 프라이버시 (SECURITY-03)
- polyline(GPS 좌표=민감 위치정보)은 **로그 본문에 남기지 않음**. 관측 로그는 식별자/집계값(userId, distanceMeters, courseId 유무)만.

---

## 2. 성능·데이터 패턴

### 2.1 조회 인덱스
- `run` 테이블에 `(userId, finishedAt)` 복합 인덱스. 목록 조회(본인 + 기간 필터 + `finishedAt DESC`)를 인덱스 스캔으로 처리.

### 2.2 경량 목록 프로젝션
- 목록 응답(`RunSummaryResponse`)은 **polyline 미포함** — id/distance/duration/finishedAt/courseName만. 대용량 좌표 직렬화를 목록에서 제거(N건 × polyline 방지).
- 상세 응답(`RunDetailResponse`)에서만 polyline 포함.

### 2.3 대용량 좌표 컬럼 (신뢰성)
- polyline 크기 상한 미설정(NQ1) 하에서 `TEXT`(≈64KB) 절단 방지를 위해 `@Column(columnDefinition = "MEDIUMTEXT")`(≈16MB). `@Convert(PolylineConverter)`(U2)로 JSON 직렬화.

### 2.4 courseName lookup (best-effort)
- 목록/상세에서 `courseId != null`이면 `CourseRepository`(U2)로 코스명 조회. 코스 부재 시 null(예외 아님). 소량 조회 — 캐시 불필요.

---

## 3. 회복성·확장성
- 외부 의존 없음 → 서킷브레이커/재시도 **미적용**(U4의 oauth 패턴은 run에 불필요).
- stateless 요청 → 앱 수평확장 무관(상속). 규모 증가 시 목록 **페이징 백로그**(NQ2), polyline 크기 상한(NQ1)은 defer.

---

## 4. 트랜잭션·예외
- 저장/조회는 단일 엔티티 트랜잭션(`@Transactional`). 부분 저장 없음.
- 예외는 `GlobalExceptionHandler`(U1-a) → 공통 `ApiResponse` 오류 래핑(400/404/500). 신규 핸들러 없음.
