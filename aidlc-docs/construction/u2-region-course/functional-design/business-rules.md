# Business Rules — U2 지역·코스조회 🌐

> 코스/지역 조회의 매핑·필터·정렬·적재 규칙.

---

## BR-U2-1. 거리 분류 매핑 (Q2=C)
- 한글 → `CourseDistance`: `단거리`→SHORT, `중거리`→MEDIUM, `장거리`→LONG.
- courses.json의 **declaredCategory·measuredCategory 둘 다 저장**.
- API 응답 `distanceCategory` 및 `?distance` 필터 기준은 **measuredCategory(실측)**.
- `declaredCategory`는 저장만 하고 응답 비노출(향후 비교/노출 대비).

## BR-U2-2. 지역 매핑 (Q3=A)
- 한글 → `Region`: `군산`→GUNSAN, `전주`→JEONJU.
- 변환 시점은 **CourseDataLoader 적재 시**(저장은 코드/enum). 조회는 이미 코드.
- 알 수 없는 지역 문자열 → 해당 코스 적재 스킵 + WARN.

## BR-U2-3. 코스 저장 형식 (Q1=A)
- 단일 `Course` 엔티티. `polyline`/`cumulativeMeters`/`waypointAnchors`는 **JSON 문자열 컬럼**(TEXT).
- 목록 조회는 JSON 컬럼 미역직렬화(요약 필드만). 상세 조회 시에만 역직렬화.

## BR-U2-4. 필터 유효성 (Q8=A)
- `?region` 값이 `Region`에 없음 → **400**(BAD_REQUEST/VALIDATION_ERROR), 메시지에 지원 값 안내.
- `?distance` 값이 `SHORT|MEDIUM|LONG`이 아님 → **400**.
- 필터 미지정(빈 값) → 필터 미적용(정상).

## BR-U2-5. 목록 정렬 (Q7=A)
- 기본 정렬 **totalMeters 오름차순**(짧은 코스 우선). (정렬 파라미터는 현 범위 밖.)

## BR-U2-6. 코스 상세 Not Found
- `GET /courses/{id}`에서 id 미존재 → **404 NOT_FOUND**(BusinessException).

## BR-U2-7. 시드 적재 멱등성 (Q4=B)
- 앱 기동 시 `data/courses.json` 로드. id 기준 **이미 존재하면 스킵(최초 1회만 삽입)**.
- 개별 코스 변환 실패 → WARN + 스킵(전체 기동 중단 없음). 적재 요약(신규/스킵 수) INFO 로그.

## BR-U2-8. 미정 필드 보존 (Q6=A)
- `searchOption`은 **저장만**, 응답 비노출. 용도 확정 시 노출.

## BR-U2-9. 지역 표시명 (Q5=A)
- `/regions` 이름(군산/전주)은 **RegionCatalog/Region enum 표시명**에서 제공. U1-a RegionCatalog을 code+표시명(+법정동)으로 확장.

## BR-U2-10. 응답 래핑 (U1-a 일관)
- 성공은 `ApiResponse.success(data)`, 실패는 GlobalExceptionHandler 경유 공통 실패 응답. 빈 목록도 200 + `[]`.
