# Logical Components (Light) — U2 지역·코스조회 🌐

> FD 도메인 + NFR 패턴을 뒷받침하는 컴포넌트 배선. 패키지 `com.ppip.dallyeo.region`, `com.ppip.dallyeo.course`.

---

## 1. 컴포넌트 목록

| 컴포넌트 | 유형 | 역할 | 비고 |
|---|---|---|---|
| `RegionController` | @RestController | `GET /regions` | 공개 |
| `RegionService` | @Service | 지원 지역 목록(code+표시명) | RegionCatalog 사용 |
| `RegionCatalog`(확장) | 상수 | code+표시명+법정동(U1-a 확장) | Q5/BR-U2-9 |
| `CourseController` | @RestController | `GET /courses`, `GET /courses/{id}` | 공개, 파라미터 검증 |
| `CourseQueryService` | @Service | 목록(필터/정렬/요약)·상세(404) | |
| `CourseRepository` | JpaRepository<Course,String> | 코스 영속성/필터 조회 | 인덱스 활용 |
| `CourseDataLoader` | ApplicationRunner | 기동 시 멱등 시드 | classpath JSON |
| `Course` | @Entity | 코스(JSON 컬럼+waypointCount) | U2 소유, U5 확장 |
| `PolylineConverter` 외 2 | AttributeConverter | JSON TEXT ↔ 객체(D1) | Jackson3 |
| `CourseDistance` | enum | SHORT/MEDIUM/LONG(한글 매핑) | |

---

## 2. 상세 배선

### Course 엔티티 (JPA)
- 필드: id(@Id), name, region/declaredCategory/measuredCategory(@Enumerated STRING), searchOption(int), totalMeters(int), **waypointCount(int, D2=B)**, polyline(List<PolylinePoint> @Convert), cumulativeMeters(List<Integer> @Convert), waypointAnchors(List<WaypointAnchor> @Convert).
- `@Table(name="course", indexes={@Index(region), @Index(measuredCategory)})`.

### AttributeConverter (D1=A)
- `PolylineConverter implements AttributeConverter<List<PolylinePoint>, String>` — Jackson3 ObjectMapper로 직렬화/역직렬화.
- `IntListConverter`(cumulativeMeters), `WaypointAnchorsConverter`(waypointAnchors) 동일 패턴.
- 공용 ObjectMapper 주입(또는 static). 변환 실패 → 예외.

### CourseRepository
- `JpaRepository<Course, String>`.
- 필터 조회: region/measuredCategory 선택 조합. 구현 선택 — (a) 조건별 파생 쿼리 + 서비스 분기, (b) `Specification`, (c) 소량이라 `findAll(Sort.by("totalMeters"))` 후 메모리 필터. 정렬 totalMeters ASC.

### CourseQueryService
- `findSummaries(Region?, CourseDistance?)` → 필터+정렬 → CourseSummary(distanceCategory=measuredCategory, waypointCount 컬럼).
- `findDetail(String id)` → findById(없으면 BusinessException NOT_FOUND) → 경로 객체(@Convert 자동 역직렬화) → CourseDetail.

### RegionService / RegionCatalog
- RegionCatalog을 code+표시명+법정동으로 확장(U1-a 것 확장). RegionService.getSupportedRegions() → List<RegionResponse>.

### CourseDataLoader
- ApplicationRunner. `data/courses.json`(classpath) 로드 → 각 코스: existsById 스킵 / 아니면 한글→enum 변환 + waypointCount 계산 + Course 저장. 결과 로깅.

---

## 3. 조회 흐름
```
GET /regions       → RegionController → RegionService → RegionCatalog → [ {code,name} ]
GET /courses       → CourseController(검증) → CourseQueryService.findSummaries → Repository(필터/정렬) → [CourseSummary]
GET /courses/{id}  → CourseController → CourseQueryService.findDetail → Repository.findById(@Convert) → CourseDetail | 404
기동             → CourseDataLoader → (existsById?스킵:저장) → course 테이블
```

## 4. U2 경계
- 신규 소유: `course` 테이블, region/course 조회 컴포넌트, RegionCatalog 표시명 확장.
- U5 확장: 동일 Course 엔티티에 사용자 코스 INSERT(CourseCommandService).
