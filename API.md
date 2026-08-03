# Dallyeo API 명세 (프론트엔드용)

> 현재까지 구현된 **공개 API**입니다. 모두 인증 없이 호출 가능(`GET`).
> 인증/사용자/러닝기록 API(🔒)는 다음 단계(U4/U5)에서 추가 예정.

- **Base URL**: `https://dallyeo.cloud` (개발 로컬: `http://localhost:8080`)
- **Content-Type**: `application/json; charset=UTF-8`
- **인증**: 현재 불필요 (아래 모든 엔드포인트 공개)

---

## 1. 공통 응답 포맷

모든 응답은 아래 래퍼로 감싸집니다.

**성공**
```json
{ "success": true, "data": { /* 또는 [ ... ] */ } }
```

**실패**
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "코스를 찾을 수 없습니다: xxx",
    "details": [ { "field": "region", "message": "..." } ]
  }
}
```
- `details`는 입력 검증 실패(`VALIDATION_ERROR`)일 때만 포함, 그 외 생략.
- 성공 시 `error` 키 없음 / 실패 시 `data` 키 없음.

### 에러 코드 (프론트 분기용 고정 문자열)
| code | HTTP | 의미 |
|---|---|---|
| `VALIDATION_ERROR` | 400 | 입력 검증 실패(필수 누락/형식 오류) — `details` 포함 |
| `BAD_REQUEST` | 400 | 잘못된 파라미터 값(지원 안 하는 region/category 등) |
| `NOT_FOUND` | 404 | 리소스 없음(없는 코스/장소) |
| `EXTERNAL_API_ERROR` | 502 | 외부 관광 API(TourAPI) 지연/오류 — **재시도 권장** |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |
| `UNAUTHORIZED` / `FORBIDDEN` / `CONFLICT` | 401/403/409 | (인증 도입 후 사용 예정) |

### 공통 enum 값
| 이름 | 값 |
|---|---|
| **region** | `GUNSAN`(군산), `JEONJU`(전주) |
| **distance**(코스 거리) | `SHORT`, `MEDIUM`, `LONG` |
| **category**(장소 카테고리) | `TOUR`, `RESTAURANT`, `CAFE`, `CULTURE`, `FESTIVAL`, `TRAVEL_COURSE`, `LEPORTS`, `STAY`, `SHOPPING`, `ETC` |
| **badges**(배지) | `MODEL_RESTAURANT`(모범음식점), `GOOD_PRICE`(착한가격업소) |

---

## 2. 지역 (Regions)

### 2.1 지역 목록 조회
```
GET /regions
```
**응답 200**
```json
{
  "success": true,
  "data": [
    { "code": "GUNSAN", "name": "군산" },
    { "code": "JEONJU", "name": "전주" }
  ]
}
```

---

## 3. 코스 (Courses)  — DB 데이터, 항상 즉시 응답

### 3.1 코스 목록 조회
```
GET /courses?region={GUNSAN|JEONJU}&distance={SHORT|MEDIUM|LONG}
```
- `region`, `distance` 모두 **선택**(생략 시 전체). 잘못된 값 → 400.
- 정렬: `totalMeters` 오름차순(짧은 코스부터). 경로 좌표는 목록에 미포함(요약).

**응답 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "jeonju-hanok-village-run",
      "name": "한옥마을 둘레길 코스",
      "region": "JEONJU",
      "distanceCategory": "SHORT",
      "totalMeters": 2799,
      "waypointCount": 5
    }
  ]
}
```

### 3.2 코스 상세 조회
```
GET /courses/{id}
```
- 지도 그리기용 경로 좌표/누적거리/경유지 포함. 없는 id → 404.

**응답 200**
```json
{
  "success": true,
  "data": {
    "id": "jeonju-hanok-village-run",
    "name": "한옥마을 둘레길 코스",
    "region": "JEONJU",
    "distanceCategory": "SHORT",
    "totalMeters": 2799,
    "polyline": [
      { "lat": 35.81409, "lng": 127.15010 },
      { "lat": 35.81412, "lng": 127.15013 }
    ],
    "cumulativeMeters": [0, 25, 58],
    "waypointAnchors": [
      { "name": "전주경기전 정문", "polylineIndex": 0 }
    ]
  }
}
```
- `polyline[i]` ↔ `cumulativeMeters[i]` 인덱스 대응(시작점부터 누적 거리, m).
- `waypointAnchors[].polylineIndex` = 해당 경유지가 위치한 `polyline` 인덱스.

---

## 4. 장소 (Places)  — 외부 관광 API(TourAPI) 기반

> ⚠️ 장소 API는 실시간 외부 API를 호출합니다. 드물게 `502 EXTERNAL_API_ERROR`가 날 수 있으니 **한 번 더 호출**하면 됩니다(두 번째부터 캐시로 빨라짐).

### 공통: 장소 목록 항목(PlaceSummary)
```json
{
  "id": "914536",
  "name": "군산 해망굴",
  "category": "TOUR",
  "latitude": 35.9755,      // null 가능(키워드/지역 목록에서 좌표 없는 경우)
  "longitude": 126.6801,
  "address": "전북특별자치도 군산시 군산창2길 48",
  "thumbnailUrl": "https://.../image.jpg",  // null 가능
  "distanceMeters": 221.9   // /places/nearby 에서만 값, 그 외 null
}
```

### 4.1 키워드 검색
```
GET /places/search?keyword={검색어}&region={GUNSAN|JEONJU}&category={카테고리}
```
- `keyword` **필수**. `region`, `category` 선택.
- 응답: `data` = PlaceSummary 배열.

### 4.2 지역 장소 목록
```
GET /places?region={GUNSAN|JEONJU}&category={카테고리}
```
- `region` **필수**. `category` 선택.
- 응답: PlaceSummary 배열.

### 4.3 주변 장소 (반경)
```
GET /places/nearby?lat={위도}&lng={경도}&radius={미터}&category={카테고리}
```
- `lat`, `lng` **필수**(숫자). `radius` 선택(기본 **1000**m, 양수). `category` 선택.
- 응답: PlaceSummary 배열(거리순, `distanceMeters` 포함).

**응답 200 (검색/목록/반경 공통)**
```json
{
  "success": true,
  "data": [
    { "id": "914536", "name": "군산 해망굴", "category": "TOUR",
      "latitude": 35.9755, "longitude": 126.6801,
      "address": "전북특별자치도 군산시 군산창2길 48",
      "thumbnailUrl": null, "distanceMeters": null }
  ]
}
```

### 4.4 장소 상세
```
GET /places/{id}
```
- `id` = 목록 응답의 `id`(TourAPI contentId). 없는 장소 → 404.

**응답 200**
```json
{
  "success": true,
  "data": {
    "id": "914536",
    "name": "군산 해망굴",
    "category": "TOUR",
    "latitude": 35.9755,
    "longitude": 126.6801,
    "address": "전북특별자치도 군산시 군산창2길 48",
    "businessHours": "상시 개방",           // null 가능(정보 없거나 미지원 타입)
    "imageUrl": "https://.../image.jpg",   // null 가능
    "badges": ["GOOD_PRICE"]                // 배지 없으면 []
  }
}
```
- `badges`: 군산 모범음식점/착한가격업소로 매칭된 경우에만 값. 대부분의 일반 장소는 `[]`.

---

## 5. 프론트 연동 시 주의사항

1. **응답은 항상 `{success, data|error}` 래퍼** — `data`/`error`를 먼저 분기.
2. **null 필드 존재** — 장소의 `latitude/longitude/thumbnailUrl/businessHours/imageUrl/distanceMeters`는 상황에 따라 `null`. UI에서 방어 처리 필요.
3. **502 재시도** — `/places/*`(검색/목록/반경/상세)는 외부 API라 간헐 502 가능 → 1~2회 재시도 로직 권장. `/regions`, `/courses*`는 DB라 502 없음.
4. **좌표축 주의** — `latitude`=위도, `longitude`=경도. 반경 조회 파라미터도 `lat`(위도)/`lng`(경도).
5. **에러 코드로 분기** — `error.code`는 고정 문자열이라 UI 분기에 사용(메시지는 변경될 수 있음).

---

## 6. 아직 미구현(다음 단계 예정)

| 기능 | 상태 |
|---|---|
| 소셜 로그인 / 토큰 발급·갱신 (🔒) | U4 예정 |
| 사용자 프로필 (🔒) | U4 예정 |
| 러닝 기록 CRUD (🔒) | U5 예정 |
| 사용자 코스 생성 `POST /courses` (🔒) | U5 예정 |
| 장소 상세 부가정보/이미지 갤러리 | 후속 |

> 인증 도입 후 🔒 엔드포인트는 `Authorization: Bearer {token}` 헤더가 필요해집니다. 현재 문서의 공개 API는 그대로 유지됩니다.
