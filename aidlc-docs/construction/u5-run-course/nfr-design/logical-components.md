# Logical Components — U5 러닝기록 🔒 (NFR Design)

> NFR 패턴(`nfr-design-patterns.md`)을 뒷받침하는 논리 컴포넌트. 도메인 타입은 `functional-design/domain-entities.md` 참조.
> U5 = run 도메인 표준 3계층. 신규 인프라 컴포넌트(캐시/서킷/외부클라이언트) 없음.

---

## 1. 컴포넌트 목록

| 논리 컴포넌트 | 유형 | 역할 | NFR 근거 |
|---|---|---|---|
| `RunController` | @RestController(`/runs`) | 저장/목록/상세 엔드포인트, `@AuthUser` 주입, `ApiResponse` 래핑 | 보안 1.1 |
| `RunService` | @Service | 검증·소유권·저장/조회 오케스트레이션, courseName lookup | 보안 1.2/1.3, 성능 2.4 |
| `RunRepository` | JPA Repository | `run` CRUD + 본인·기간·정렬 조회 | 성능 2.1 |
| `Run` | @Entity | 러닝 기록(`(userId, finishedAt)` 인덱스, polyline MEDIUMTEXT) | 성능 2.1/2.3 |
| `RunCreateRequest` | DTO | 저장 요청 + 검증 | 보안 1.3 |
| `RunSummaryResponse` | DTO | 목록 요약(**polyline 제외**) | 성능 2.2 |
| `RunDetailResponse` | DTO | 상세(polyline 포함, completionRate=null) | 기능 |
| `CourseRepository`(U2 재사용) | JPA Repository | courseName lookup(읽기) | 성능 2.4 |
| `PolylinePoint`/`PolylineConverter`(U2 재사용) | DTO/Converter | 좌표 JSON 직렬화 | 성능 2.3 |
| `@AuthUser`/`AuthUserArgumentResolver`(U4 재사용) | ArgumentResolver | 현재 userId 주입 | 보안 1.1 |
| `GlobalExceptionHandler`(U1-a 재사용) | @RestControllerAdvice | 400/404/500 공통 래핑 | 예외 4 |

**신규 파일**: `Run`, `RunRepository`, `RunService`, `RunController`, `RunCreateRequest`, `RunSummaryResponse`, `RunDetailResponse` (+ 테스트). 그 외 전부 재사용.

---

## 2. 컴포넌트별 상세

### 2.1 RunController (`/runs`)
- `POST /runs` → `RunService.save(userId, RunCreateRequest)` → 201 `ApiResponse<RunDetailResponse>`.
- `GET /runs?from&to` → `RunService.list(userId, from, to)` → 200 `ApiResponse<List<RunSummaryResponse>>`.
- `GET /runs/{id}` → `RunService.getDetail(userId, id)` → 200 `ApiResponse<RunDetailResponse>`.
- `from`/`to`는 `@RequestParam(required=false)` ISO date. 파싱 실패 → 400.

### 2.2 RunService
- **save**: 검증(BR-U5-5) → `Run` 빌드(userId 귀속, courseId 느슨, createdAt) → `RunRepository.save` → DetailResponse(courseName lookup, completionRate=null).
- **list**: from/to → Instant 범위 변환 → `RunRepository` 조회 → SummaryResponse 매핑(courseName best-effort).
- **getDetail**: `findById` → 없으면 404 → `userId` 소유권 확인(불일치 404) → DetailResponse.
- courseName lookup 헬퍼: `courseId==null ? null : courseRepository.findById(courseId).map(name).orElse(null)`.

### 2.3 RunRepository
- `extends JpaRepository<Run, Long>`.
- 조회 쿼리(기간 optional): 파생 메서드 조합 또는 `@Query`로
  - `userId = :userId AND (:from IS NULL OR finishedAt >= :from) AND (:to IS NULL OR finishedAt <= :to) ORDER BY finishedAt DESC`.
- 대안: from/to 조합별 파생 메서드(`findByUserIdOrderByFinishedAtDesc`, `findByUserIdAndFinishedAtBetween...`). Code 단계에서 택1(단일 `@Query` 권장).

### 2.4 Run 엔티티 매핑
- `@Id @GeneratedValue(IDENTITY) Long id`.
- `@Table(name="run", indexes=@Index(name="idx_run_user_finished", columnList="userId, finishedAt"))`.
- `@Convert(PolylineConverter) @Column(columnDefinition="MEDIUMTEXT") List<PolylinePoint> polyline`.
- `userId Long`(not null), `courseId String`(nullable), `distanceMeters/durationSeconds/averagePaceSeconds int`, `startedAt/finishedAt/createdAt Instant`, `@PrePersist createdAt=now`.

---

## 3. 컴포넌트 상호작용

```
POST /runs
  RunController.save(@AuthUser userId, RunCreateRequest)
    └─ RunService.save
         ├─ validate(request)                      [400 on violation]
         ├─ Run 빌드(userId, courseId?, polyline, ...)
         ├─ RunRepository.save                      [MySQL, MEDIUMTEXT polyline]
         └─ DetailResponse(courseName lookup, completionRate=null)  → 201

GET /runs?from&to
  RunController.list(@AuthUser userId, from?, to?)
    └─ RunService.list
         ├─ RunRepository (userId + 기간 + finishedAt DESC)   [idx_run_user_finished]
         └─ map→SummaryResponse(polyline 제외, courseName best-effort) → 200

GET /runs/{id}
  RunController.detail(@AuthUser userId, id)
    └─ RunService.getDetail
         ├─ findById → 없으면 404
         ├─ run.userId == userId? 아니면 404(은폐)
         └─ DetailResponse(polyline 포함) → 200
```

---

## 4. 산출 경계
- **U5 실제 구현**: 위 신규 7파일 + 테스트. Course/보안/예외/컨버터는 재사용(수정 없음).
- **후속(defer)**: 목록 페이징(NQ2), polyline 크기 상한(NQ1), 완주율 계산, 러닝 수정/삭제, 통계.
- **연계 참고**: U4에서 언급된 "계정삭제 cascade(runs 제거)"는 현재 U5 범위 밖(러닝 삭제 defer) — 계정 하드삭제 시 runs 처리 정책은 Build&Test 또는 후속에서 확인 필요.
