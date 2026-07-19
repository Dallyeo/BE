# Code Generation Summary — U2 지역·코스조회 🌐

> **검증**: `compileJava`/`compileTestJava` 성공, U2 단위테스트(Region/Course/Converter/QueryService/Controller/DataLoader) 전부 통과. CourseDataLoaderTest는 실제 `data/courses.json`(10코스)로 매핑·멱등 검증. 전체 빌드/통합(@SpringBootTest·MySQL)은 Build & Test.

---

## 1. 생성/수정 파일

### 수정(Modified)
- `src/main/java/com/ppip/dallyeo/domain/region/RegionCatalog.java` — 표시명(군산/전주) + `displayName`/`supportedRegions`/`fromDisplayName` 추가(U1-a `find`/LDongCode 호환 유지).
- `src/main/resources/data/courses.json` — courses.json을 classpath 리소스로 배치(신규 복사).

### 생성(Created) — 앱 코드
| 패키지 | 파일 | 역할 |
|---|---|---|
| domain.region | RegionResponse, RegionService, RegionController | `GET /regions`(US-REGION-1) |
| course | CourseDistance | 거리 enum + 한글/파라미터 파서(BR-U2-1) |
| course.dto | PolylinePoint, WaypointAnchor, CourseSummary, CourseDetail | 경로 VO + 응답 DTO |
| course.converter | PolylineConverter, IntListConverter, WaypointAnchorsConverter | JSON TEXT ↔ 객체 @Convert(D1, Jackson3) |
| course | Course(@Entity), CourseRepository | 코스 영속(JSON 컬럼/waypointCount/인덱스) |
| course | CourseQueryService | 목록(필터/정렬/요약)·상세(404) |
| course | CourseController | `GET /courses`, `GET /courses/{id}`(400/404) |
| course | CourseDataLoader | 기동 멱등 시드(ApplicationRunner) |

### 생성(Created) — 테스트
- RegionServiceTest, CourseDistanceTest, CourseConvertersTest, CourseQueryServiceTest, CourseControllerTest, CourseDataLoaderTest

---

## 2. 스토리 추적 (완료)
| 스토리 | 구현 |
|---|---|
| US-REGION-1 지역 목록 | RegionController/Service + RegionCatalog 표시명 ✅ |
| US-COURSE-1 코스 목록/필터 | CourseController(필터 400) + QueryService(totalMeters ASC/요약) + DataLoader ✅ |
| US-COURSE-2 코스 상세 | QueryService.findDetail(@Convert 역직렬화/404) ✅ |

---

## 3. 설계 반영 확인
- Q1=A JSON 컬럼 + D1=A @Convert 컨버터 3종(Jackson3) / D2=B waypointCount 컬럼(목록 시 경로 미역직렬화).
- Q2=C declared·measured 둘 다 저장, 응답/필터=measured. / Q3=A 적재 시 한글→enum. / Q4=B 멱등(existsById 스킵).
- Q6=A searchOption 저장·비노출. / Q7=A totalMeters ASC. / Q8=A 잘못된 필터 400. / R2 인덱스 region·measuredCategory.

## 4. 기술 메모
- **Jackson3**: 컨버터/로더 모두 `tools.jackson.databind.ObjectMapper`. Jackson3 예외는 unchecked라 try-catch 불필요(로더 load()만 리소스 IO 방어).
- **필터 쿼리**: `CourseRepository.findByFilters`(JPQL, null=미적용). 소량이나 인덱스/쿼리로 U5 성장 대비.
- **AttributeConverter**: Hibernate가 인스턴스화 → ObjectMapper 정적 공유.
- **ddl-auto=update**: `course` 테이블 자동 생성(I1=A). Build & Test에서 실제 MySQL 기동·시드 검증 필요.

## 5. 확장 지점 (U5)
- 사용자 코스 생성(`POST /courses`)은 동일 `Course` 엔티티에 INSERT(CourseCommandService). waypointCount/JSON 컬럼 재사용.
- 캐시 도입 시 목록/상세 + 생성 무효화 전략 재검토(현재 미적용).
