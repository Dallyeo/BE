# Domain Entities / DTOs — U2 지역·코스조회 🌐

> 확정 답변: Q1=A(JPA 1엔티티+JSON 컬럼), Q2=C(declared/measured 둘 다 저장·응답은 measured), Q3=A(적재 시 코드 변환), Q4=B(최초 1회 적재), Q5=A(RegionCatalog 표시명), Q6=A(searchOption 저장·비노출), Q7=A(totalMeters ASC), Q8=A(잘못된 필터 400).
> U1-a 재사용: `Region` enum, `RegionCatalog`, `ApiResponse`, `BusinessException/ErrorCode`.

---

## 1. 영속 엔티티 (JPA)

### Course (테이블 `course`) — Q1=A
```
Course
  - String id            @Id            // courses.json "id" (예: gunsan-modern-history-run)
  - String name                          // 코스명
  - Region region        @Enumerated(STRING)   // 한글→enum 변환 저장(Q3)
  - CourseDistance declaredCategory @Enumerated(STRING)  // 선언 분류 (저장, Q2=C)
  - CourseDistance measuredCategory @Enumerated(STRING)  // 실측 분류 (응답/필터 기준, Q2=C)
  - int searchOption                     // 용도 미정 — 저장만, 응답 비노출(Q6)
  - int totalMeters                      // 총 거리(m)
  - String polylineJson       @Lob/TEXT  // [{lat,lng}...] JSON 직렬화
  - String cumulativeMetersJson @Lob/TEXT// [int...] JSON 직렬화
  - String waypointAnchorsJson  @Lob/TEXT// [{name,polylineIndex}...] JSON 직렬화
```
- 대용량 경로 데이터(polyline ~400점 등)는 **JSON 문자열 컬럼**으로 저장(정규화 자식 테이블 없음).
- 목록 조회는 JSON 컬럼을 역직렬화하지 않고 요약 필드만 사용(성능).
- 상세 조회 시에만 JSON 3종을 역직렬화해 응답.

> **소유 유닛**: U2가 `course` 테이블을 소유. U5(사용자 코스 생성)가 동일 엔티티에 INSERT로 확장.

---

## 2. Enum / 값 객체

```
enum CourseDistance { SHORT, MEDIUM, LONG }
  // 한글 매핑: 단거리→SHORT, 중거리→MEDIUM, 장거리→LONG
  // API 파라미터/응답 값도 SHORT/MEDIUM/LONG
```
- `Region`(GUNSAN, JEONJU) — U1-a 재사용. RegionCatalog에 **표시명**(군산/전주) 추가(Q5).

---

## 3. 응답 DTO

### RegionResponse — `GET /regions`
```
RegionResponse { String code; String name; }   // 예: {"GUNSAN","군산"}
```

### CourseSummary — `GET /courses` 목록(요약, 경로 제외)
```
CourseSummary
  - String id
  - String name
  - Region region              // 코드
  - CourseDistance distanceCategory   // = measuredCategory (Q2=C)
  - int totalMeters
  - int waypointCount          // waypointAnchors 개수
```

### CourseDetail — `GET /courses/{id}` 상세(경로 포함)
```
CourseDetail
  - String id, name
  - Region region
  - CourseDistance distanceCategory   // = measuredCategory
  - int totalMeters
  - List<PolylinePoint> polyline      // [{lat,lng}...]
  - List<Integer> cumulativeMeters
  - List<WaypointAnchor> waypointAnchors

PolylinePoint  { double lat; double lng; }
WaypointAnchor { String name; int polylineIndex; }
```
> `searchOption`, `declaredCategory`는 응답 DTO에 **미노출**(Q2=C/Q6). 엔티티에는 보존.

---

## 4. 시드 적재 모델 (CourseDataLoader)
- courses.json 원본 파싱용 임시 모델(내부): `id,name,region(한글),searchOption,declaredCategory(한글),measuredCategory(한글),polyline,cumulativeMeters,totalMeters,waypointAnchors`.
- 적재 시 한글 region/category → enum 변환(Q3), polyline/cumulative/waypoints → JSON 문자열로 직렬화해 Course 엔티티로 저장.
- 위치: `src/main/resources/data/courses.json`(classpath).
