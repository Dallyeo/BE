# Code Generation Plan — U2 지역·코스조회 🌐

> **U2 Code Generation의 단일 진실 소스**. Part 2에서 순서대로 실행, 각 단계 완료 시 즉시 `[x]`.
> Brownfield / Spring Boot 4 / Java 17. Base package `com.ppip.dallyeo`. 앱 코드 `src/main/java/...`, 테스트 `src/test/java/...`.

---

## 유닛 컨텍스트

### 구현 스토리
- **US-REGION-1** 지역 목록 조회 (`GET /regions`)
- **US-COURSE-1** 지역별 코스 목록 조회/필터 (`GET /courses?region&distance`)
- **US-COURSE-2** 코스 상세 조회 (`GET /courses/{id}`)

### 의존/경계
- 의존: U1-a(`ApiResponse`, `BusinessException/ErrorCode`, `Region`, `RegionCatalog`). 인증 불필요(공개).
- 소유: `course` 테이블(U5가 확장). RegionCatalog은 U1-a 것을 **표시명 추가로 확장**(in-place 수정).
- 설계: FD(business-logic-model/business-rules/domain-entities), NFR(@Convert 컨버터 3종, waypointCount 컬럼, 인덱스), Infra(MySQL/ddl-auto=update, classpath 시드).

### 패키지 구조
```
com.ppip.dallyeo.domain.region/   (U1-a 기존 + U2 추가)
   Region, LDongCode, RegionCatalog(확장), RegionCodeMapper   ← 기존
   RegionResponse, RegionService, RegionController            ← U2 신규
com.ppip.dallyeo.course/
   Course(@Entity), CourseDistance(enum)
   dto/       CourseSummary, CourseDetail, PolylinePoint, WaypointAnchor
   converter/ PolylineConverter, IntListConverter, WaypointAnchorsConverter
   CourseRepository, CourseQueryService, CourseController, CourseDataLoader
```

---

## 실행 단계 (Part 2)

### Step 1 — 지역 표시명 확장  [US-REGION-1]
- [x] `RegionCatalog`(domain.region) in-place 수정: code+표시명(군산/전주)+법정동. 표시명 조회 메서드 추가(BR-U2-9). 기존 `find(Region)`/LDongCode 유지(U1-a 호환).

### Step 2 — 지역 조회 레이어 생성  [US-REGION-1]
- [x] `domain/region/RegionResponse`(code,name), `RegionService`(getSupportedRegions), `RegionController`(`GET /regions`).

### Step 3 — 지역 조회 테스트  [US-REGION-1]
- [x] `RegionServiceTest` — 군산/전주 표시명 목록.

### Step 4 — 거리 분류 enum  [US-COURSE-1] (BR-U2-1)
- [x] `course/CourseDistance`(SHORT/MEDIUM/LONG) + 한글 매핑(단/중/장거리) 파서.
- [x] `CourseDistanceTest` — 한글→enum, 미지원 처리.

### Step 5 — 경로 값 객체 + AttributeConverter  [US-COURSE-2] (D1)
- [x] `course/dto/PolylinePoint`(lat,lng), `course/dto/WaypointAnchor`(name,polylineIndex).
- [x] `course/converter/PolylineConverter`, `IntListConverter`, `WaypointAnchorsConverter` (Jackson3 ObjectMapper, @Converter).
- [x] `ConverterTest` — 직렬화/역직렬화 왕복.

### Step 6 — Course 엔티티 + Repository  [US-COURSE-1/2] (BR-U2-3, R2)
- [x] `course/Course`(@Entity, @Table indexes region/measuredCategory): id/name/region/declared·measuredCategory/searchOption/totalMeters/waypointCount + polyline·cumulativeMeters·waypointAnchors(@Convert).
- [x] `course/CourseRepository`(JpaRepository<Course,String>).

### Step 7 — 코스 응답 DTO  [US-COURSE-1/2]
- [x] `course/dto/CourseSummary`(id,name,region,distanceCategory,totalMeters,waypointCount), `CourseDetail`(+polyline,cumulativeMeters,waypointAnchors).

### Step 8 — CourseQueryService  [US-COURSE-1/2] (BR-U2-4/5/6)
- [x] `course/CourseQueryService`: findSummaries(region?,distance?)[필터+totalMeters ASC+요약], findDetail(id)[404]. distanceCategory=measuredCategory.
- [x] `CourseQueryServiceTest` — 필터/정렬/요약/404 (@DataJpaTest 또는 Mockito).

### Step 9 — CourseController  [US-COURSE-1/2] (BR-U2-4)
- [x] `course/CourseController`: `GET /courses`(region/distance 파싱·잘못된 값 400), `GET /courses/{id}`.
- [x] `CourseControllerTest`(@WebMvcTest) — 목록/상세/400/404 응답 형태.

### Step 10 — CourseDataLoader (시드)  [US-COURSE-1] (BR-U2-7, Q4=B)
- [x] `course/CourseDataLoader`(ApplicationRunner): classpath `data/courses.json` 로드 → id 멱등 삽입(존재 스킵), 한글→enum 변환, waypointCount 계산, 변환실패 WARN+스킵, 결과 INFO.
- [x] `CourseDataLoaderTest` — 파싱/변환/멱등(2회 실행 시 중복 없음) (@DataJpaTest).

### Step 11 — 시드 데이터 리소스 배치
- [x] `aidlc-docs/.../courses.json` → `src/main/resources/data/courses.json` 복사(classpath).

### Step 12 — 빌드/컴파일 검증
- [x] `./gradlew compileJava compileTestJava` 성공 확인, 인프라 불필요 단위테스트 스모크.

### Step 13 — 코드 요약 문서
- [x] `aidlc-docs/construction/u2-region-course/code/code-summary.md` — 생성/수정 파일, 스토리 추적, U5 확장 지점.

---

## 산출물 위치
- 앱 코드: `src/main/java/com/ppip/dallyeo/{domain/region,course}/...`, 리소스 `src/main/resources/data/courses.json`
- 문서: `aidlc-docs/construction/u2-region-course/code/`

## 범위 밖 / 이관
- 사용자 코스 생성(POST /courses) — U5(CourseCommandService)
- 캐시 — 미적용(R1)
- 인증 — 공개 유닛(해당 없음)
