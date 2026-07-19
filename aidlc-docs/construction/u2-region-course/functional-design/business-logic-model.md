# Business Logic Model — U2 지역·코스조회 🌐

> 공개 조회 유닛. 지역 목록 + 코스 목록/상세 + 시드 적재 흐름. 모든 응답은 U1-a `ApiResponse`로 래핑.

---

## 1. 지역 목록 조회 (US-REGION-1) — `GET /regions`
```
RegionController.list()
  → RegionService.getSupportedRegions()
       → RegionCatalog의 정의(코드+표시명)로 List<RegionResponse> 생성
  → ApiResponse.success([{code,name}...])   // 200
```
- 고정 목록(군산/전주). 외부 의존/DB 없음.

---

## 2. 코스 목록 조회 (US-COURSE-1) — `GET /courses?region&distance`
```
CourseController.list(region?, distance?)
  1) 파라미터 파싱/검증: region→Region, distance→CourseDistance
       - 잘못된 값 → 400 (BR-U2-4)
  2) CourseQueryService.findSummaries(region, distance)
       - CourseRepository 조회 + 필터(region 일치, measuredCategory 일치)
       - 정렬: totalMeters ASC (BR-U2-5)
       - Course → CourseSummary 매핑(distanceCategory=measuredCategory, waypointCount=anchors 수)
  3) ApiResponse.success([CourseSummary...])   // 200 (빈 목록도 200 + [])
```
- 필터는 둘 다 선택. 미지정 시 전체.
- 목록은 **JSON 경로 컬럼을 역직렬화하지 않음**(요약 필드만). waypointCount는 적재 시 계산 저장 또는 anchors JSON 길이만 산출(구현 선택 — 성능 위해 별도 카운트 컬럼 고려 가능).

---

## 3. 코스 상세 조회 (US-COURSE-2) — `GET /courses/{id}`
```
CourseController.detail(id)
  → CourseQueryService.findDetail(id)
       - CourseRepository.findById(id)
       - 없으면 → BusinessException(NOT_FOUND) → 404 (BR-U2-6)
       - polylineJson/cumulativeMetersJson/waypointAnchorsJson 역직렬화
       - Course → CourseDetail 매핑
  → ApiResponse.success(CourseDetail)   // 200
```

---

## 4. 시드 적재 흐름 (CourseDataLoader) — Q4=B 최초 1회
```
앱 기동 시 (ApplicationRunner/CommandLineRunner)
  1) classpath `data/courses.json` 로드 → 파싱(내부 시드 모델 리스트)
  2) 각 코스에 대해:
       - CourseRepository.existsById(id) ?
           예 → 스킵(기존 보존)               // Q4=B: 최초 1회만
           아니오 → 변환 후 저장:
               region 한글→Region(BR-U2-2), category 한글→CourseDistance(BR-U2-1)
               polyline/cumulativeMeters/waypointAnchors → JSON 문자열 직렬화
               Course INSERT
  3) 적재 결과(신규/스킵 수) INFO 로그
```
- 멱등: 재기동해도 이미 있으면 스킵 → 중복 없음. (내용 갱신이 필요하면 수동/향후 Upsert 옵션.)
- 파싱/변환 실패(알 수 없는 region/category) → 해당 코스 **WARN 로그 + 스킵**(전체 기동 실패 방지).

---

## 5. 데이터 흐름 요약
```
courses.json(resources) ──기동 적재(1회)──▶ course 테이블(JSON 컬럼)
                                                │
GET /regions   ← RegionCatalog(고정)            │
GET /courses   ← 필터/정렬/요약 매핑 ───────────┤
GET /courses/{id} ← findById + JSON 역직렬화 ───┘
```
