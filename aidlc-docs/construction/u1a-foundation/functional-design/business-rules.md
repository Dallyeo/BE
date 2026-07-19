# Business Rules — U1-a 공개 기반

> U1-a의 규칙: 에러코드 체계, 검증, 정규화, 카테고리 폴백, 캐시/회복성.

---

## BR-1. 에러 코드 체계 (F1=A)
- `error.code`는 **문자열 도메인 코드**(대문자 스네이크). 예: `VALIDATION_ERROR`, `UNAUTHORIZED`, `NOT_FOUND`, `EXTERNAL_API_ERROR`.
- 베이스 코드(§business-logic-model §2)는 공통. 도메인별 특화 코드는 각 유닛에서 정의하되 **베이스 HTTP status와 정합** 유지.
- 코드/메시지 분리: `code`는 프론트 분기용(고정), `message`는 사람이 읽는 설명(변경 가능).

## BR-2. 검증 실패 응답 (F2=A)
- Bean Validation 실패 → HTTP 400, `code=VALIDATION_ERROR`.
- `error.details`에 **필드별 오류 목록**: `[{ field, message }]`.
- 여러 필드 오류 시 전부 포함.

## BR-3. TourAPI 정규화 규칙 (F3=A)
- **BR-3.1** 모든 응답 필드의 빈 문자열 `""` → `null` 정규화.
- **BR-3.2** `mapx`(경도)/`mapy`(위도)/`dist`(거리) 문자열 → `double` 파싱. (X=경도, Y=위도 주의)
- **BR-3.3 좌표 파싱 실패 시 엔드포인트별 정책:**
  | 오퍼레이션 | 좌표 실패 처리 |
  |---|---|
  | `locationBasedList2` (반경) | 좌표 **필수** → 해당 항목 **제외** |
  | `areaBasedList2` | 좌표 **null 유지**, 항목 **보존** |
  | `searchKeyword2` | 좌표 **null 유지**, 항목 **보존** |
- **BR-3.4** 항목 제외 또는 파싱 실패 발생 시 **로그로 카운트/사유 기록**(운영 가시성).

## BR-4. 카테고리 매핑·폴백 (F4=A)
- **BR-4.1** 매핑: `contentTypeId` → `CategoryType`
  - 12→`TOUR`, 39→`RESTAURANT`, 39+`lclsSystm2=FD05`→`CAFE`, 14→`CULTURE`, 15→`FESTIVAL`, 25→`TRAVEL_COURSE`, 28→`LEPORTS`, 32→`STAY`, 38→`SHOPPING`.
- **BR-4.2** 미매핑/애매한 `contentTypeId` → **`ETC` 폴백**.
- **BR-4.3** 폴백/모든 경우 **원본 `contentTypeId`를 `rawContentTypeId`로 보존** (모델: `PlaceCategory(CategoryType type, int rawContentTypeId)`).
- **BR-4.4** 미매핑 폴백 발생 시 **WARN 로그**를 남긴다. (조용한 폴백 금지 — 신규 타입 등장 인지 목적. 로그에 원본 typeId 포함)

## BR-5. RegionCatalog 규칙 (F5=A)
- 지역은 **코드 상수**로 정의(현재 `GUNSAN`, `JEONJU`).
- 매핑: `GUNSAN`=(52,130), `JEONJU`=(52,110). 전북 시도코드 52 고정.
- 알 수 없는 Region 요청 → 400 `BAD_REQUEST`.

## BR-6. 캐시 정책 (F6=C)
- **캐시 키**: `op명 + 정규화된 파라미터 해시`.
- **TTL**: **30분+** (부하 최소 우선). TourAPI 관광/음식 데이터는 저빈도 변경이라 장기 캐시 허용.
- 캐시 대상: TourAPI 조회 응답(목록/상세). 개인화/인증 데이터는 캐시 금지.

## BR-7. 회복성 정책 (Resilience4j)
- **타임아웃**: 외부 호출 상한(예: connect/read 수 초). 초과 → 실패 처리.
- **재시도**: 일시 오류(5xx/타임아웃)에 제한적 재시도(지수 백오프).
- **서킷브레이커**: 실패율 임계 초과 시 오픈 → 즉시 `EXTERNAL_API_ERROR`(502) 폴백, 반개방으로 회복 탐지.
- 구체 수치(타임아웃 ms/재시도 횟수/임계율)는 NFR Design에서 확정.
