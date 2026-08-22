# U5 러닝기록 🔒 — Code Generation Plan

> **단일 진실원천(Single Source of Truth)**. Part 2는 이 플랜의 스텝 순서대로만 실행.
> **프로젝트**: Brownfield, Spring Boot, 패키지 `com.ppip.dallyeo`. 코드 위치 = workspace root `src/main/java`, 테스트 `src/test/java`.

---

## 유닛 컨텍스트

- **스토리**: US-RUN-1(저장), US-RUN-2(목록), US-RUN-3(상세). (US-COURSE-3 제거됨.)
- **의존**: U4(`@AuthUser`/`AuthUserArgumentResolver`, JWT 보안), U2(`PolylinePoint`/`PolylineConverter`, `CourseRepository` 읽기).
- **소유 엔티티**: `run` 테이블(신규). Course/User 변경 없음.
- **신규 인프라/의존성/시크릿**: 없음. `/runs/**`는 deny-by-default 자동 포함 → SecurityConfig 변경 없음.

## 신규 패키지 구조 (`src/main/java/com/ppip/dallyeo/run/`)
```
run/
├─ Run.java                 (@Entity)
├─ RunRepository.java       (JpaRepository)
├─ RunService.java          (@Service)
├─ RunController.java       (@RestController /runs)
└─ dto/
   ├─ RunCreateRequest.java   (record + validation)
   ├─ RunSummaryResponse.java (record, polyline 제외)
   └─ RunDetailResponse.java  (record, polyline 포함, completionRate=null)
```

---

## 실행 스텝 (Part 2에서 순차 실행)

- [x] **Step 1 — Run 엔티티** (`run/Run.java`): Long id(IDENTITY), userId, courseId(nullable), polyline(@Convert PolylineConverter, `columnDefinition="MEDIUMTEXT"`), distanceMeters/durationSeconds/averagePaceSeconds, startedAt/finishedAt/createdAt(@PrePersist). `@Table(name="run", indexes=@Index(name="idx_run_user_finished", columnList="userId, finishedAt"))`. Lombok(@Getter/@Builder/@NoArgs/@AllArgs). [US-RUN-1]
- [x] **Step 2 — RunRepository** (`run/RunRepository.java`): `JpaRepository<Run, Long>` + `@Query` 기간 optional 조회(`userId` + `(:from IS NULL OR finishedAt>=:from)` + `(:to IS NULL OR finishedAt<=:to)` ORDER BY finishedAt DESC). [US-RUN-2]
- [x] **Step 3 — DTOs** (`run/dto/`): `RunCreateRequest`(courseId?, polyline, distanceMeters, durationSeconds, averagePaceSeconds, startedAt, finishedAt + @NotNull/@NotEmpty/@Positive), `RunSummaryResponse`(id, courseName, distanceMeters, durationSeconds, finishedAt), `RunDetailResponse`(id, courseId, courseName, polyline, distanceMeters, durationSeconds, averagePaceSeconds, completionRate[Double=null], startedAt, finishedAt). [US-RUN-1/2/3]
- [x] **Step 4 — RunService** (`run/RunService.java`, @Service): 
  - `save(userId, RunCreateRequest)`: 검증(BR-U5-5: polyline 비어있지않음, distance/duration>0, finishedAt>=startedAt, averagePace 존재) → Run 빌드(userId 귀속, courseId 느슨) → save → DetailResponse.
  - `list(userId, from, to)`: LocalDate→Instant 변환(파싱 실패 400) → repository 조회 → SummaryResponse(courseName best-effort).
  - `getDetail(userId, id)`: findById 없으면 404 → 소유권(userId 불일치 404) → DetailResponse.
  - courseName lookup 헬퍼(CourseRepository, null-safe). completionRate=null(Q6). [US-RUN-1/2/3]
- [x] **Step 5 — RunController** (`run/RunController.java`, `/runs`): `POST`(@ResponseStatus CREATED, @Valid @RequestBody, @AuthUser), `GET`(@RequestParam from/to required=false, @AuthUser), `GET /{id}`(@PathVariable, @AuthUser). 전부 `ApiResponse` 래핑. [US-RUN-1/2/3]
- [x] **Step 6 — 단위 테스트**:
  - `RunServiceTest`: 저장 검증 성공/실패(빈 polyline·음수·시간역전 400), courseId 느슨(미존재도 저장), 목록 소유자 격리+courseName lookup, 상세 소유권(타인 404·미존재 404), completionRate null. (Mockito+AssertJ)
  - `RunControllerTest`: @WebMvcTest 또는 standalone MockMvc — POST 201, GET 목록/상세, 검증 400. (U4 컨트롤러 테스트 패턴 준용)
  - [US-RUN-1/2/3]
- [x] **Step 7 — 컴파일 & 테스트 검증**: `./gradlew compileJava compileTestJava` + U5 테스트 실행(통과 확인).
- [x] **Step 8 — 코드 요약 문서** (`aidlc-docs/construction/u5-run-course/code/code-summary.md`): 생성 파일 목록/스토리 매핑/검증 결과.
- [x] **Step 9 — API 문서 갱신** (`API.md`): `/runs` 3개 엔드포인트 추가(요청/응답 예시), "U5 예정" 표기 → 완료로 갱신.
- [x] **Step 10 — Postman 컬렉션 갱신** (`dallyeo-postman-collection.json`): runs 3요청 추가(POST/GET/GET{id}, Bearer 토큰 변수 사용).

## 변경 없음(확인용)
- `SecurityConfig`(deny-by-default 자동 적용), `build.gradle`(신규 의존성 없음), `application.properties`(신규 키 없음), `Course`/`User` 엔티티.

## 스토리 추적
| 스토리 | 구현 스텝 |
|---|---|
| US-RUN-1 완료 러닝 기록 저장 | Step 1,3,4,5 |
| US-RUN-2 러닝 기록 목록 조회 | Step 2,3,4,5 |
| US-RUN-3 러닝 기록 상세 조회 | Step 3,4,5 |
