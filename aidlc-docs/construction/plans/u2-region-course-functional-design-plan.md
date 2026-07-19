# Functional Design Plan — U2 지역·코스조회 🌐

U2의 비즈니스 로직/도메인 모델/규칙을 확정하기 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **U2 범위**: 공개 API. 스토리 US-REGION-1(지역 목록), US-COURSE-1(코스 목록/필터), US-COURSE-2(코스 상세).
> **컴포넌트**: RegionController/RegionService/RegionCatalog, CourseController/CourseQueryService/CourseRepository/CourseDataLoader.
> **의존**: U1-a(ApiResponse/예외/Region·RegionCatalog). 인증 불필요.
> **데이터 원본**: `courses.json` 10개 고정 코스. 필드: id, name, region(한글), searchOption, declaredCategory/measuredCategory(단·중·장), polyline[{lat,lng}], cumulativeMeters[int], totalMeters, waypointAnchors[{name,polylineIndex}].
> **API(api-spec §3~4)**: `GET /regions` / `GET /courses?region&distance` (목록=요약) / `GET /courses/{id}` (상세=경로포함, 없으면 404).

---

## Part A. 실행 체크리스트
- [x] 유닛 컨텍스트 분석 (스토리/컴포넌트/데이터)
- [x] 결정 질문 수집(Part B)
- [x] `business-logic-model.md` — 지역/코스 조회 흐름, 시드 적재
- [x] `business-rules.md` — 필터/거리분류/매핑/404 규칙
- [x] `domain-entities.md` — Course 엔티티/DTO, Region 응답

---

## Part B. 결정 질문

### Question Q1 — 코스 영속화 방식
10개 고정 코스(각 polyline 최대 ~400점 + cumulativeMeters + waypointAnchors)를 어떻게 저장/서빙할까요?

A) **JPA 1엔티티 + 대용량 필드를 JSON 문자열 컬럼**(polyline/cumulativeMeters/waypointAnchors를 JSON TEXT) — 읽기전용 10개라 단순 (권장)
B) **정규화** — Course + CoursePoint + Waypoint 자식 테이블(조인)
C) **DB 미사용** — 기동 시 courses.json을 메모리 로드해 서빙(읽기전용이라 충분, CourseRepository/Loader는 인메모리 구현)
X) Other

[Answer]: A

---

### Question Q2 — 거리 분류(distanceCategory) 기준
courses.json에는 `declaredCategory`(선언)와 `measuredCategory`(실측)가 있습니다. API의 `distanceCategory`/필터는 무엇 기준?

A) **measuredCategory(실측)** 사용 (권장, 실제 경로 기반)
B) **declaredCategory(선언)** 사용
C) 둘 다 저장, 응답/필터엔 measured
X) Other

[Answer]: C

---

### Question Q3 — 한글 → 코드 변환 위치
region "군산"→`GUNSAN`, 거리 "중거리"→`MEDIUM` 변환을 어디서?

A) **CourseDataLoader가 적재 시 코드/enum으로 변환**해 저장(조회는 이미 코드) (권장)
B) 조회 응답 시 변환(원본 한글 저장)
X) Other

[Answer]: A

---

### Question Q4 — 시드 적재(기동 시 idempotency)
앱 기동 시 courses.json 적재 정책은? (courses.json은 `src/main/resources`로 복사해 classpath 로드)

A) **Upsert** — id 기준 있으면 갱신/없으면 삽입(항상 최신 반영) (권장)
B) **최초 1회만** — 이미 있으면 스킵(기존 보존)
C) **전량 삭제 후 재적재** — 매 기동 초기화
X) Other

[Answer]: B

---

### Question Q5 — `/regions` 표시명 소스
지역 목록 응답 `{code, name}`의 한글 이름(군산/전주)을 어디서?

A) **RegionCatalog/Region enum에 표시명 추가** — U1-a RegionCatalog을 확장(code+한글명+법정동), RegionService가 목록 제공 (권장)
B) 별도 설정/DB 테이블로 관리
X) Other

[Answer]: A

---

### Question Q6 — 미정 필드(searchOption / declared vs measured) 처리
api-spec에서 "용도 미정"인 `searchOption`, 그리고 응답에 안 쓰는 카테고리를 어떻게?

A) **내부 저장만(응답 비노출)** — 용도 확정 시 노출, 데이터는 보존 (권장)
B) **적재 제외** — 저장하지 않음
X) Other

[Answer]: A

---

### Question Q7 — 코스 목록 기본 정렬
`GET /courses` 목록 정렬 기본값은?

A) **totalMeters 오름차순**(짧은 코스부터) (권장)
B) 이름 가나다순
C) courses.json 원본 순서 유지
X) Other

[Answer]: A

---

### Question Q8 — 필터 유효성(잘못된 region/distance 값)
`?region=XXX` 또는 `?distance=YYY`에 잘못된 값이 오면?

A) **400 오류**(VALIDATION_ERROR/BAD_REQUEST) — 지원하지 않는 값 명시 (권장)
B) 잘못된 필터는 무시하고 전체/유효필터만 적용
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u2-region-course/functional-design/business-logic-model.md`
- [x] `aidlc-docs/construction/u2-region-course/functional-design/business-rules.md`
- [x] `aidlc-docs/construction/u2-region-course/functional-design/domain-entities.md`
