# Dallyeo API 명세 (프론트엔드용)

> 현재까지 구현된 API입니다. 🌐 = 공개(토큰 불필요), 🔒 = 인증 필요.
> **U1-a/U2/U3(공개 조회) + U4(인증·사용자) + U5(러닝 기록) + U6(업적)** 완료.
> **최근 변경**: 코스 경유지에서 자동 생성 이름(`경유지1` 등) 제거 · 러닝 기록 이미지 업로드(`POST /runs/{id}/image`) 추가 · 코스 `imageUrl` 추가 · `/places` **목록 3종에 `badges` 추가** · `/places` **목록 3종에 `businessHours`/`openHours` 추가**(원문 정리 + 대표 영업시간 분리) · `POST /runs` 응답에 **`newAchievements` 추가**(결과창 도장, 최초 달성만) · 업적 **8종 → 21종 확장** + 응답에 `category`/`sortOrder`/`iconOnUrl`/`iconOffUrl` 추가. 사용자 코스 생성은 백엔드에 저장하지 않음(프론트/클라이언트 담당) — 사용자가 만든 경로는 러닝 기록의 `polyline`으로 저장됩니다.

- **Base URL**: `https://dallyeo.cloud` (개발 로컬: `http://localhost:8080`)
- **Content-Type**: `application/json; charset=UTF-8`
- **인증**: 🌐 공개 API는 토큰 불필요. 🔒 보호 API는 `Authorization: Bearer {accessToken}` 헤더 필요.
  - 정책: deny-by-default — 공개 화이트리스트(로그인/갱신·지역·코스·장소)만 열리고, 그 외는 토큰 없으면 `401`.
  - Access Token 만료 24시간 / Refresh Token 만료 7일. 만료 시 `POST /auth/refresh`로 재발급(회전).

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
      "description": "전주 한옥마을 일대를 가볍게 한 바퀴 도는 코스입니다. 곳곳에 자리한 크고 작은 문화유산을 둘러보며 전주의 정취를 느껴보세요.",
      "imageUrl": "/images/courses/jeonju-hanok-village-run.png",
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
    "description": "전주 한옥마을 일대를 가볍게 한 바퀴 도는 코스입니다. 곳곳에 자리한 크고 작은 문화유산을 둘러보며 전주의 정취를 느껴보세요.",
    "imageUrl": "/images/courses/jeonju-hanok-village-run.png",
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
- `waypointAnchors`는 **실제 지명만** 담습니다. 자동 생성 이름(`경유지1`, `경유지2` …)은 제거되었습니다.
  경로(`polyline`)는 그대로이고 표시할 지점 수만 줄어든 것이라, 지도에 그리는 선은 이전과 동일합니다.
- `imageUrl`: 코스 대표 이미지. **서버 기준 절대 경로**이므로 API 베이스 URL을 앞에 붙여 사용하세요
  (예: `https://dallyeo.cloud/images/courses/xxx.png`). 이미지가 아직 없는 코스는 필드가 **생략**됩니다.

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
  "businessHours": "12:00~21:00\n준비시간 14:00~17:00\n마지막 주문 20:30",  // null 가능
  "openHours": "12:00~21:00",   // businessHours의 첫 항목(대표값). null 가능
  "thumbnailUrl": "https://.../image.jpg",  // null 가능
  "distanceMeters": 221.9,  // /places/nearby 에서만 값, 그 외 null
  "badges": ["MODEL_RESTAURANT"]  // 배지 없으면 [] (상세와 동일 형태)
}
```
- `badges`는 목록(4.1/4.2/4.3)과 상세(4.4)가 **동일한 형태**입니다.
  목록 카드에 배지 칩을 그리려고 항목마다 `/places/{id}`를 추가 호출할 필요가 없습니다.
- **`businessHours` / `openHours`** (목록·상세 동일 형태)
  - `businessHours`: TourAPI 원문을 정리한 **전체 영업시간**. 항목 구분은 **개행(`\n`)** 입니다.
    원문의 `<br>`·HTML 태그·글머리표(`- `)는 서버에서 제거합니다.
  - `openHours`: 그중 **첫 항목**만 뽑은 대표 영업시간. 카드 한 줄(`63km · 12:00~21:00`)에는 이 값을 쓰세요.
  - 정보가 없거나 조회하지 못하면 둘 다 `null` → 카드에서는 그 자리를 비우면 됩니다.
  - ⚠️ 목록의 영업시간은 항목마다 외부 상세 API를 호출해 채웁니다. 캐시가 비어 있는 첫 호출은 **3~5초**,
    이후 같은 조건은 **0.1초 미만**입니다(군산 음식점 109건 실측: 콜드 4.5s / 웜 0.05s).
    드물게 첫 호출에서 일부가 `null`로 올 수 있고 다시 호출하면 채워지므로, `null`을 "영업시간 없음"으로
    확정하지 말고 그냥 빈 자리로 처리해 주세요.

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
      "businessHours": null, "openHours": null,
      "thumbnailUrl": null, "distanceMeters": null, "badges": [] }
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
    "businessHours": "상시 개방",           // null 가능(정보 없음). 여러 항목이면 개행(\n) 구분
    "openHours": "상시 개방",               // businessHours의 첫 항목(대표값). null 가능
    "imageUrl": "https://.../image.jpg",   // null 가능
    "badges": ["GOOD_PRICE"]                // 배지 없으면 []
  }
}
```
- `badges`: 군산 모범음식점(`MODEL_RESTAURANT`)/착한가격업소(`GOOD_PRICE`)로 매칭된 경우에만 값.
  대부분의 일반 장소는 `[]`입니다.
- **매칭 범위**: 배지 원천은 군산시 공공데이터 2종(모범음식점 52건 + 착한가격업소 60건, 총 97건 적재)입니다.
  매칭은 정규화한 **업소명과 주소가 둘 다 정확히 일치**할 때만 성립하므로(오매칭 방지), TourAPI 표기가
  조금이라도 다르면 배지가 붙지 않습니다. 히트율이 낮은 건 데이터 부족이 아니라 이 보수적 규칙 때문입니다.

---

## 5. 인증 (Auth)

> 소셜 로그인으로 토큰을 발급받아 🔒 API를 호출합니다. 로그인/갱신은 공개(🌐), 로그아웃은 보호(🔒).

### 5.1 소셜 로그인 🌐
```
POST /auth/login/{provider}
```
- `provider`: `kakao` | `apple` (그 외 → 400)
- 소셜 계정을 검증해 회원을 **생성 또는 조회**하고 토큰을 발급합니다.
  - **kakao**: `authorizationCode` = 프론트가 받은 **카카오 access token** → 서버가 카카오 사용자 조회로 검증
  - **apple**: `authorizationCode` = **identity token(JWT)** → 서버가 애플 공개키(JWKS)로 검증

**Request Body**
```json
{ "authorizationCode": "소셜 access token 또는 identity token" }
```

**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "jwt...",
    "refreshToken": "jwt...",
    "tokenType": "Bearer",
    "accessTokenExpiresIn": 86400,
    "onboardingRequired": true,
    "user": {
      "id": 1,
      "nickname": "러너3821",
      "gender": "NONE",
      "height": null,
      "weight": null,
      "profileImageUrl": null
    }
  }
}
```
- `accessTokenExpiresIn`: Access Token 만료(초) = 86400(24h).
- `onboardingRequired`: 신체정보(키/체중) 미입력이면 `true` → 프론트가 온보딩 화면으로 분기.
- 신규 회원의 `nickname`: 소셜 닉네임이 있으면 사용, 없으면 자동생성(`러너####`).
- 소셜 검증 실패/무효 → `401`(UNAUTHORIZED).

### 5.2 토큰 갱신 🌐
```
POST /auth/refresh
```
- Access Token 만료 시 Refresh Token으로 새 토큰을 재발급합니다(재로그인 불필요).

**Request Body**
```json
{ "refreshToken": "jwt..." }
```
**Response 200**
```json
{
  "success": true,
  "data": {
    "accessToken": "jwt...",
    "refreshToken": "jwt...",
    "tokenType": "Bearer",
    "accessTokenExpiresIn": 86400
  }
}
```
- **회전(rotation)**: 갱신 시 새 refreshToken도 함께 발급되며 **이전 refreshToken은 즉시 무효**. 응답의 새 값으로 교체 저장하세요.
- Refresh Token 만료/불일치 → `401` → 재로그인 유도.

### 5.3 로그아웃 🔒
```
POST /auth/logout
```
- 서버에 저장된 내 Refresh Token을 무효화합니다. (Access Token은 만료 전까지 형식상 유효)
- **Response 204** (본문 없음)

---

## 6. 사용자 (Users) 🔒

> 모두 인증 필요(`Authorization: Bearer {accessToken}`). 본인 데이터만 접근.

### 6.1 내 프로필 조회
```
GET /users/me
```
**Response 200**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "러너3821",
    "gender": "MALE",
    "height": 178.0,
    "weight": 72.0,
    "profileImageUrl": null
  }
}
```
- `gender`: `MALE` | `FEMALE` | `NONE`
- `profileImageUrl`: 저장 방식 미정으로 현재 항상 `null`.

### 6.2 프로필/온보딩 수정
```
PATCH /users/me
```
- 온보딩 신체정보 입력과 설정 수정을 겸합니다. **전달한 필드만 부분 갱신**되고, 호출 시 온보딩 완료로 처리됩니다(이후 로그인 `onboardingRequired=false`).

**Request Body** (수정할 필드만 전송, 전부 선택)
```json
{
  "nickname": "러너제인",
  "gender": "FEMALE",
  "height": 165.0,
  "weight": 55.0
}
```
- 유효성: `nickname` 1~20자, `height` 50~250(cm), `weight` 20~300(kg), `gender`는 enum. 위반 시 `400`.
- **온보딩 건너뛰기**: 빈 바디 `{}` 전송 → 신체정보는 그대로, 온보딩만 완료 처리.

**Response 200** — 수정된 프로필(6.1과 동일 구조).

### 6.3 계정 삭제(탈퇴)
```
DELETE /users/me
```
- 본인 계정을 완전 삭제(하드 삭제)하고 저장된 Refresh Token도 제거합니다.
- **Response 204** (본문 없음)

---

## 7. 러닝 기록 (Runs) 🔒  — 본인 기록만

> 클라이언트가 추적을 끝낸 **완료 데이터**만 저장합니다(실시간 소켓 없음). 모든 요청에 `Authorization: Bearer {accessToken}` 필요.

### 7.1 러닝 기록 저장
```
POST /runs
```
**Request Body**
```json
{
  "courseId": "gunsan-modern-history-run | null",
  "polyline": [ { "lat": 35.95, "lng": 126.68 } ],
  "distanceMeters": 10480,
  "durationSeconds": 3600,
  "averagePaceSeconds": 343,
  "startedAt": "2026-07-09T07:00:00Z",
  "finishedAt": "2026-07-09T08:00:00Z"
}
```
- `courseId`: 시드(공식) 코스를 달렸으면 참조, 자유 러닝/직접 만든 경로면 `null`. 존재 검증은 하지 않습니다.
- `polyline` 비어있음 / `distanceMeters`·`durationSeconds` ≤ 0 / 필수 필드 누락 / `finishedAt < startedAt` → **400**

**Response 201**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "courseId": "gunsan-modern-history-run | null",
    "courseName": "근대 역사 박물관 런 | null",
    "polyline": [ { "lat": 35.95, "lng": 126.68 } ],
    "distanceMeters": 10480,
    "durationSeconds": 3600,
    "averagePaceSeconds": 343,
    "imageUrl": null,
    "startedAt": "2026-07-09T07:00:00Z",
    "finishedAt": "2026-07-09T08:00:00Z",
    "newAchievements": [
      { "code": "JJAMPPONG", "name": "짬뽕을 먹을 자격이 있는 자",
        "description": "군산 짬뽕거리 코스를 완주한 사람",
        "unlocked": true, "unlockedAt": "2026-07-09T08:00:01Z" }
    ]
  }
}
```
> `completionRate`(완주율)는 아직 계산하지 않습니다(응답에서 생략). 계산 기준 확정 후 추가 예정.
> `imageUrl`은 저장 직후엔 항상 비어 있습니다 — 이미지는 **7.2에서 별도로 업로드**합니다.

#### 🏅 `newAchievements` — 러닝 결과창 도장
- 이 러닝으로 **처음 달성한** 업적만 담깁니다. **결과창에 띄울 도장이 바로 이 배열**입니다.
- **재달성은 절대 다시 오지 않습니다.** 같은 조건을 몇 번 더 채워도 두 번째부터는 항상 `[]` 입니다
  (서버가 미달성 업적만 판정하고, DB에도 `(userId, achievement)` 유니크 제약이 있습니다).
- 새로 달성한 게 없으면 **빈 배열** `[]` 입니다(필드는 항상 존재).
- **저장(`POST /runs`) 응답에만 있습니다.** 조회(`GET /runs/{id}`, `GET /runs`)에는 이 필드 자체가 없어서,
  지난 기록을 다시 열어도 도장이 재생되지 않습니다.
- 항목 형태는 업적 목록(8.1)과 동일합니다 — 같은 `code`로 도장 이미지를 매칭하면 됩니다.

### 7.2 러닝 기록 이미지 업로드
```
POST /runs/{id}/image
Content-Type: multipart/form-data
```
러닝을 저장(7.1)해서 받은 `id`로 기록 이미지를 올립니다. **2단계 흐름**입니다:
`POST /runs` 로 기록 저장 → 응답의 `id` → `POST /runs/{id}/image` 로 이미지 첨부.

**Request (multipart/form-data)**

| 파트명 | 타입 | 설명 |
|---|---|---|
| `image` | file | 이미지 파일 (**필수**) |

- 허용 형식: `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`. 그 외 → **400**
- 최대 크기: **10MB**. 초과 → **400**
- 본인 기록만. 타인의 기록이거나 존재하지 않으면 **404**
- 이미 이미지가 있으면 **새 파일로 교체**되고 이전 파일은 서버에서 삭제됩니다(재호출로 재업로드 가능).

**Response 200** — 7.1과 동일 구조이며 `imageUrl`이 채워져 돌아옵니다.
```json
{
  "success": true,
  "data": {
    "id": 1,
    "courseId": "gunsan-modern-history-run | null",
    "courseName": "근대 역사 박물관 런 | null",
    "polyline": [ { "lat": 35.95, "lng": 126.68 } ],
    "distanceMeters": 10480,
    "durationSeconds": 3600,
    "averagePaceSeconds": 343,
    "imageUrl": "/uploads/runs/3f2a....jpg",
    "startedAt": "2026-07-09T07:00:00Z",
    "finishedAt": "2026-07-09T08:00:00Z"
  }
}
```
- `imageUrl`은 **서버 기준 절대 경로**입니다. 표시할 때 API 베이스 URL을 앞에 붙이세요
  (예: `https://dallyeo.cloud/uploads/runs/3f2a....jpg`).
- 이미지 조회는 **인증 없이** 가능합니다(파일명이 UUID라 URL을 모르면 접근 불가).
- 파일명은 서버가 UUID로 새로 짓습니다 — 클라이언트가 보낸 파일명은 사용되지 않습니다.

### 7.3 러닝 기록 목록 조회
```
GET /runs?from={ISO date}&to={ISO date}
```
- 본인 기록만 반환. `from`/`to`(예: `2026-07-01`)는 선택 — `finishedAt` 기준으로 필터, **최신순** 정렬.
- 잘못된 날짜 형식 → **400**. 목록 응답은 경량(**polyline 미포함**).

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "courseName": "근대 역사 박물관 런 | null",
      "distanceMeters": 10480,
      "durationSeconds": 3600,
      "imageUrl": "/uploads/runs/3f2a....jpg",
      "finishedAt": "2026-07-09T08:00:00Z"
    }
  ]
}
```

### 7.4 러닝 기록 상세 조회
```
GET /runs/{id}
```
- 본인 기록만. 타인의 기록이거나 존재하지 않으면 **404**.
- **Response 200** — 7.1 응답과 동일 구조(`polyline`·`imageUrl` 포함).

---

## 8. 업적 (Achievements) 🔒  — 본인 기준

> 러닝 기록을 기반으로 달성되는 업적 **21종**. 모든 요청에 `Authorization: Bearer {accessToken}` 필요.

### 도장 이미지
- `iconOnUrl`(획득=컬러) / `iconOffUrl`(미획득=흑백)을 **둘 다** 내려줍니다. `unlocked` 값으로 골라 쓰세요.
- 경로는 서버 절대경로이고 **인증 없이** 접근됩니다. 앞에 API base URL을 붙이면 됩니다
  (예: `https://dallyeo.cloud/images/achievements/jjamppong_on.webp`).
- 포맷은 **WebP** 450×450 투명배경. 21종 × 2상태 = 42장.

### 분류(`category`)
- `GUNSAN` / `JEONJU` / `COMMON`. **`COMMON`은 지역 무관 업적**이라
  코드 접두사로 지역을 판정하면 안 됩니다. 화면 지역 탭은 이 값으로 분기하세요.
- `sortOrder`는 시안 순서(군산 → 전주 → 공통)이며, 목록은 **이미 이 순서로 정렬돼** 내려갑니다.

### 업적 코드 목록 (21종)
| code | 분류 | 업적명 | 달성 조건 |
|---|---|---|---|
| `GUNSAN_SEONYUDO` | GUNSAN | 선유도 짱 | 선유도 해변 런 완주 |
| `GUNSAN_CONQUEROR` | GUNSAN | 군산 런트립 정복자 | 군산 추천 코스 전부 완주 |
| `JJAMPPONG` | GUNSAN | 짬뽕을 먹을 자격이 있는 자 | 짬뽕런 완주 |
| `GUNSAN_BEGINNER` | GUNSAN | 군산 초보 러너 | 군산 코스 1회 이상 완주 |
| `NATURE_LOVER` | GUNSAN | 자연을 사랑해! | 편백나무 숲 런 완주 |
| `BETWEEN_WAVES` | GUNSAN | 부숴지는 파도들 사이에서 | 새만금 방파제 런 완주 |
| `JEONJU_CHERRY_BLOSSOM` | JEONJU | 천변벚꽃 | ⏸ 해당 코스 없음 — 기준 미확정 |
| `JEONJU_BEGINNER` | JEONJU | 전주 초보 러너 | 전주 코스 1회 이상 완주 |
| `JEONJU_DEOKJIN_LAKE` | JEONJU | 덕진 호수 | ⏸ 해당 코스 없음 — 기준 미확정 |
| `JEONJU_CONQUEROR` | JEONJU | 전주 런트립 정복자 | 전주 추천 코스 전부 완주 |
| `JEONJU_PILGRIM` | JEONJU | 전주 성지순례자 | 천주교 성지 코스 완주 |
| `JEONJU_ECO_MUSEUM` | JEONJU | 전주 자연생태관 | ⏸ 해당 코스 없음 — 기준 미확정 |
| `LONG_RUN_3H` | COMMON | 장기간 러닝 성공 | 단일 러닝 3시간 초과 |
| `FINISH_10` | COMMON | 완주 10회 달성 | 누적 러닝 10회(자유 러닝 포함) |
| `ICE_CREAM_RUNNER` | COMMON | 아이스크림 러너 | 한국시간 기준 12월에 완주 |
| `DISTANCE_100KM` | COMMON | 100km 이상 | 누적 거리 100km 이상 |
| `EARLY_BIRD` | COMMON | 얼리버드 | 한국시간 00:00~08:00 사이 시작 |
| `WAYPOINT_3` | COMMON | 경유지 3개 지나감 | 경유지 3개 이상 코스 완주 |
| `REST_TIME` | COMMON | 휴식타임 | ⏸ 도착지 정보 미수신 — 판정 불가 |
| `SLOW_WALKER` | COMMON | 뚜벅이 | ⏸ 기준 페이스 미확정 |
| `PIONEER` | COMMON | 개척자 | courseId 없는 러닝 완주 |

> ⏸ 표시된 **5종은 판정 기준이 아직 확정되지 않아 자동 달성되지 않습니다.** 목록에는 나오지만
> 항상 `unlocked: false` 입니다. 기준이 정해지면 서버만 고치면 되고 응답 형태는 그대로입니다.

> 업적은 `POST /runs`로 러닝을 저장할 때 **서버가 자동으로 판정·달성**합니다.
> **새로 달성한 업적은 저장 응답의 `newAchievements`로 바로 내려갑니다(7.1 참고)** — 결과창 도장은 그 값을 쓰세요.
> 목록(8.1)을 다시 부를 필요가 없습니다.

### 8.1 업적 목록 조회
```
GET /achievements
```
- 전체 21종 + 본인 달성 여부/일시. **`sortOrder` 오름차순으로 정렬된 상태**로 내려갑니다.

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "code": "JJAMPPONG",
      "category": "GUNSAN",
      "sortOrder": 30,
      "name": "짬뽕을 먹을 자격이 있는 자",
      "description": "군산의 짬뽕거리 코스를 완주했다.",
      "iconOnUrl": "/images/achievements/jjamppong_on.webp",
      "iconOffUrl": "/images/achievements/jjamppong_off.webp",
      "unlocked": true,
      "unlockedAt": "2026-07-09T07:35:10Z"
    },
    {
      "code": "SLOW_WALKER",
      "category": "COMMON",
      "sortOrder": 200,
      "name": "뚜벅이",
      "description": "완주시 페이스가 키로당 몇분",
      "iconOnUrl": "/images/achievements/slow_walker_on.webp",
      "iconOffUrl": "/images/achievements/slow_walker_off.webp",
      "unlocked": false,
      "unlockedAt": null
    }
  ]
}
```

### 8.2 업적 수동 달성
```
POST /achievements/{code}/unlock
```
- 특정 업적을 수동으로 달성 요청. 서버가 조건을 재판정합니다.
- 조건 충족 → 달성(8.1 항목 구조 반환). 이미 달성 → 그대로 반환(멱등).
- 조건 미충족 → **409**, 없는 code → **404**.

**Response 200** — 해당 업적 항목(`unlocked: true`).

---

## 9. 프론트 연동 시 주의사항

1. **응답은 항상 `{success, data|error}` 래퍼** — `data`/`error`를 먼저 분기.
2. **null 필드 존재** — 장소의 `latitude/longitude/thumbnailUrl/businessHours/openHours/imageUrl/distanceMeters`는 상황에 따라 `null`. UI에서 방어 처리 필요.
   - 러닝 `imageUrl`(업로드 전)과 코스 `imageUrl`(이미지 미등록)도 마찬가지로 비어 있을 수 있습니다.
   - 반면 `badges`는 **항상 배열**입니다 — 매칭이 없으면 `null`이 아니라 `[]`.
3. **502 재시도** — `/places/*`(검색/목록/반경/상세)는 외부 API라 간헐 502 가능 → 1~2회 재시도 로직 권장. `/regions`, `/courses*`는 DB라 502 없음.
4. **좌표축 주의** — `latitude`=위도, `longitude`=경도. 반경 조회 파라미터도 `lat`(위도)/`lng`(경도).
5. **에러 코드로 분기** — `error.code`는 고정 문자열이라 UI 분기에 사용(메시지는 변경될 수 있음).
6. **🔒 요청엔 토큰 첨부** — `Authorization: Bearer {accessToken}` 헤더. 없거나 만료면 `401`.
7. **401 → 자동 갱신 흐름** — 🔒 요청이 `401`이면 `POST /auth/refresh`로 재발급 후 원요청 재시도. refresh도 401이면 재로그인.
8. **토큰 회전 저장** — `/auth/refresh` 응답의 **새 refreshToken으로 반드시 교체** 저장(이전 값은 무효).
9. **이미지 경로는 상대경로** — 러닝 `imageUrl`(`/uploads/...`)과 코스 `imageUrl`(`/images/...`)은 서버 기준 절대 경로입니다. 표시할 때 Base URL을 붙이세요. 이미지 조회 자체는 토큰이 필요 없습니다.

---

## 10. 아직 미구현(다음 단계 예정)

| 기능 | 상태 |
|---|---|
| 소셜 로그인 / 토큰 발급·갱신 (🔒) | ✅ **U4 완료** |
| 사용자 프로필·온보딩·탈퇴 (🔒) | ✅ **U4 완료** |
| 러닝 기록 저장·목록·상세 `POST/GET /runs` (🔒) | ✅ **U5 완료** |
| 러닝 기록 이미지 업로드 `POST /runs/{id}/image` (🔒) | ✅ **완료** |
| 결과창 업적 도장 `newAchievements` (🔒) | ✅ **완료**(최초 달성만, 저장 응답 전용) |
| 업적 21종 + 도장 이미지 서빙 | ✅ **완료**(16종 자동판정 / 5종 기준 미확정) |
| 코스 대표 이미지 `imageUrl` | ⏳ 필드/서빙 완료 — **이미지 파일 수급 대기** |
| `/places` 목록 3종 `badges` | ✅ **완료**(상세와 동일 형태) |
| `/places` 목록 3종 `businessHours` | ✅ **완료**(상세와 동일 형태, 개행 구분) |
| 대표 영업시간 `openHours` 분리 | ✅ **완료**(목록·상세 모두) |
| 업적 목록·달성 `GET /achievements`, `POST /achievements/{code}/unlock` (🔒) | ✅ **U6 완료** |
| 사용자 코스 생성 `POST /courses` (🔒) | 백엔드 저장 안 함(프론트 담당) — 러닝 `polyline`으로 보관 |
| 러닝 기록 수정·삭제 / 완주율·통계 / 업적 진행률 | 후속(백로그) |
| 장소 상세 부가정보/이미지 갤러리 | 후속 |
| 러닝 이미지 삭제 API / 배지 매칭 완화(이름만·유사도) | 후속(백로그) |

> 🔒 엔드포인트는 `Authorization: Bearer {token}` 헤더가 필요합니다. 공개 API는 그대로 유지됩니다.
