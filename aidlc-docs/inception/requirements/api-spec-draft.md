# Dallyeo 백엔드 API 명세 (초안 v0.1)

> **대상 독자**: 프론트엔드 개발자 (및 그 AI 어시스턴트)
> **목적**: 백엔드가 제공할 REST API를 정의. 프론트는 이 명세에 맞춰 연동.
> **상태**: **초안** — 확정된 기능만 포함. 미결 항목(회의 후 확정)은 문서 하단 "이번 초안 제외" 참고.
> **주의**: 경로/필드명은 제안값이며 협의로 조정 가능. 확정 후 v1.0으로 승격.

---

## 0. 공통 규약 (Conventions)

- **Base URL**: 루트(`/`) — 프리픽스 없음 (예: `POST /auth/login/kakao`)
- **포맷**: 요청/응답 모두 `application/json` (UTF-8)
- **인증**: `Authorization: Bearer {accessToken}` 헤더
  - 🔒 = 토큰 필요, 🌐 = 공개(토큰 불필요)
- **좌표**: `latitude`(위도), `longitude`(경도) — `double`

### 0.1 공통 응답 래퍼
> 구조는 제안값(협의 후 확정). 성공/실패를 구분하고 HTTP status code를 의미에 맞게 사용.

**성공 (데이터 있음) — HTTP 200**
```json
{
  "success": true,
  "data": { }
}
```

**성공 (반환 데이터 없음) — HTTP 204 No Content** (본문 없음)

**실패 — 상황별 HTTP status (400/401/403/404/409/500 등)**
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "사람이 읽을 수 있는 설명"
  }
}
```

### 0.2 주요 HTTP status 사용 기준
| status | 사용 시점 |
|---|---|
| 200 | 조회/처리 성공 + 반환 데이터 있음 |
| 201 | 리소스 생성 성공 (코스/러닝기록 생성 등) |
| 204 | 성공했으나 반환 본문 없음 (삭제/로그아웃 등) |
| 400 | 잘못된 요청 / 검증 실패 |
| 401 | 미인증(토큰 없음/만료) |
| 403 | 권한 없음(타인 리소스 접근 등) |
| 404 | 리소스 없음 |
| 409 | 충돌(중복 등) |

### 0.3 공통 Enum (제안)
- **Region(지역)**: `GUNSAN`(군산), `JEONJU`(전주) — 지역 확장 시 추가
- **CourseDistance(거리 분류)**: `SHORT`(단거리), `MEDIUM`(중거리), `LONG`(장거리)
- **Gender(성별)**: `MALE`, `FEMALE`, `NONE`(미입력)
- **PlaceCategory(장소 분류)**: `RESTAURANT`(음식점), `CAFE`(카페), `TOUR`(관광지), `CONVENIENCE`(편의시설) — 값 확정 필요(❓)

---

## 1. 인증 (Auth)

### 1.1 소셜 로그인 🌐
- **`POST /auth/login/{provider}`** — `provider`: `kakao` | `apple`
- 소셜 인가 정보로 로그인/가입 후 토큰 발급

**Request Body**
```json
{
  "authorizationCode": "소셜 인가 코드 또는 identity token"
}
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
      "id": "long",
      "nickname": "string",
      "gender": "MALE | FEMALE | NONE",
      "height": 175.0,
      "weight": 68.0,
      "profileImageUrl": "string | null"
    }
  }
}
```
- `onboardingRequired`: 신규 사용자 또는 신체정보 미입력 시 `true` → 프론트가 온보딩(V1)으로 분기
- Access Token 만료 24시간, Refresh Token 만료 7일 (Refresh는 서버 Redis 저장)

### 1.2 토큰 갱신 🌐
- **`POST /auth/refresh`**

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
- Refresh Token 무효/만료 시 401 → 재로그인 유도

### 1.3 로그아웃 🔒
- **`POST /auth/logout`**
- 서버의 Refresh Token 무효화
- **Response 204** (본문 없음)

---

## 2. 사용자 / 프로필 (Users) — V1 온보딩, V13 설정

### 2.1 내 프로필 조회 🔒
- **`GET /users/me`**

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": "long",
    "nickname": "string",
    "gender": "MALE | FEMALE | NONE",
    "height": 175.0,
    "weight": 68.0,
    "profileImageUrl": "string | null"
  }
}
```

### 2.2 프로필/신체정보 수정 🔒 (온보딩 입력 + 설정 수정 공용)
- **`PATCH /users/me`**
- 온보딩 건너뛰기 가능 → 모든 필드 선택(nullable)

**Request Body** (수정할 필드만 전송)
```json
{
  "nickname": "string",
  "gender": "MALE | FEMALE | NONE",
  "height": 175.0,
  "weight": 68.0
}
```
**Response 200** — 수정된 프로필 반환 (2.1과 동일 구조)

> 프로필 사진 업로드는 저장 방식 미정 → 이번 초안 제외 (하단 참고)

### 2.3 계정 삭제 🔒
- **`DELETE /users/me`**
- **Response 204**

---

## 3. 지역 (Regions) — V2

### 3.1 지역 목록 조회 🌐
- **`GET /regions`**

**Response 200**
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

## 4. 코스 (Courses) — V2 메인, V7 경로수정, V8 코스확인

> 데이터 원본: `data/courses/courses.json` (10개 고정 코스). 아래 스키마는 그 구조 기반.

### 4.1 코스 목록 조회 🌐
- **`GET /courses?region={code}&distance={SHORT|MEDIUM|LONG}`**
- `region`, `distance`는 선택 필터
- 목록은 요약 정보(경로 좌표 제외)

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "gunsan-modern-history-run",
      "name": "근대 역사 박물관 런",
      "region": "GUNSAN",
      "distanceCategory": "MEDIUM",
      "totalMeters": 10604,
      "waypointCount": 7
    }
  ]
}
```

### 4.2 코스 상세 조회 🌐
- **`GET /courses/{id}`**
- 경로 좌표/누적거리/경유지 포함 (지도 그리기용)

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": "gunsan-modern-history-run",
    "name": "근대 역사 박물관 런",
    "region": "GUNSAN",
    "distanceCategory": "MEDIUM",
    "totalMeters": 10604,
    "polyline": [
      { "lat": 35.9553991, "lng": 126.6891768 }
    ],
    "cumulativeMeters": [0, 25, 58],
    "waypointAnchors": [
      { "name": "은파호수공원", "polylineIndex": 0 }
    ]
  }
}
```
- 없는 코스 → 404

### 4.3 사용자 코스 생성 🔒 — V7
- **`POST /courses`**
- 프론트가 Tmap으로 생성한 경로 JSON을 수신하여 저장 (백엔드는 Tmap 호출 안 함)

**Request Body**
```json
{
  "name": "나만의 코스",
  "region": "GUNSAN",
  "polyline": [ { "lat": 35.95, "lng": 126.68 } ],
  "cumulativeMeters": [0, 30],
  "totalMeters": 5230,
  "waypointAnchors": [ { "name": "출발지", "polylineIndex": 0 } ]
}
```
**Response 201** — 생성된 코스(4.2 구조)
- `polyline` 비어있거나 유효하지 않으면 400

> ❓ 사용자 코스도 `cumulativeMeters`/`waypointAnchors`를 프론트가 채워 보내는지 회의에서 확정

---

## 5. 러닝 기록 (Runs) — V9 진행, V10 완주결과, V11/V12 기록

### 5.1 러닝 기록 저장 🔒
- **`POST /runs`**
- 클라이언트가 추적한 **완료 데이터**만 저장 (실시간 소켓 없음)

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
- `courseId`: 기존 코스를 달렸으면 참조, 자유 러닝이면 null

**Response 201**
```json
{
  "success": true,
  "data": {
    "id": "long",
    "courseId": "gunsan-modern-history-run | null",
    "distanceMeters": 10480,
    "durationSeconds": 3600,
    "averagePaceSeconds": 343,
    "completionRate": 0.98,
    "startedAt": "2026-07-09T07:00:00Z",
    "finishedAt": "2026-07-09T08:00:00Z"
  }
}
```
> `completionRate`(완주율)은 코스 기반 러닝일 때 계산. 계산 기준(거리 대비 등)은 회의 확정 필요(❓)

### 5.2 러닝 기록 목록 조회 🔒
- **`GET /runs?from={ISO date}&to={ISO date}`**
- 본인 기록만. 기간 필터 선택

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "long",
      "courseName": "근대 역사 박물관 런",
      "distanceMeters": 10480,
      "durationSeconds": 3600,
      "finishedAt": "2026-07-09T08:00:00Z"
    }
  ]
}
```

### 5.3 러닝 기록 상세 조회 🔒 — V12
- **`GET /runs/{id}`**
- 본인 기록만 (타인 → 403/404)

**Response 200** — 5.1 응답 + `polyline` 포함

---

## 6. 장소 (Places) — V3 지도, V4/V5 검색, V6 위치정보

> 프론트 기구현 API(`/places/search`, `/places/{id}`)를 기반으로 정리. 데이터는 공공데이터포털 기반.

### 6.1 장소 검색 🌐 — V4/V5
- **`GET /places/search?keyword={k}&region={code}&category={PlaceCategory}`**

**Response 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "string",
      "name": "string",
      "category": "RESTAURANT",
      "latitude": 35.95,
      "longitude": 126.68,
      "address": "전북 군산시 ...",
      "thumbnailUrl": "string | null"
    }
  ]
}
```

### 6.2 장소 상세 🌐 — V6
- **`GET /places/{id}`**

**Response 200**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "name": "string",
    "category": "RESTAURANT",
    "latitude": 35.95,
    "longitude": 126.68,
    "address": "전북 군산시 ...",
    "businessHours": "09:00-21:00",
    "imageUrl": "string | null",
    "badges": ["MODEL_RESTAURANT", "GOOD_PRICE"]
  }
}
```
> `badges`: 모범음식점/착한가격업소 여부. 파일 데이터 적재 후 활성화 (미적재 시 빈 배열)

### 6.3 주변 장소 조회 🌐 — V3 지도, V10 완주결과
- **`GET /places/nearby?lat={}&lng={}&radius={m}&category={PlaceCategory}`**
- 좌표 기준 반경 내 장소 (관광지/음식점/편의시설/카페)

**Response 200** — 6.1과 동일한 목록 구조 (+ `badges`)

> ❓ 기본 반경(`radius`) 값: 먹거리 추천 1km vs 완주결과 500m 논의 중 → 파라미터로 받되 기본값은 회의 후 확정. "시 단위 추천"과 "반경 근접 추천" 병존 여부도 확정 필요.

---

## 7. 이번 초안 제외 (회의 후 확정 → 추가 예정)

| 기능 | 화면 | 사유 |
|---|---|---|
| 유사 검색어 / 최근 검색어 | V4/V5 | 서버 저장 vs 클라 로컬 미정 |
| 기간별 통계 (집계) | V10/V11 | 통계 항목/집계 기준 미정 |
| 완주율 계산 기준 | V10 | 계산식 미정 (엔드포인트엔 필드만 선반영) |
| 공유용 결과 데이터 | V10 | 형식 미정 |
| 프로필 사진 업로드 | V13 | 이미지 저장소(S3 등) 미정 |
| 이용약관 / 개인정보 처리방침 | V13 | 백엔드 API vs 앱 정적 미정 |
| 업적 (목록/상세) | V14 | 세부 미정 |
| 근접 추천 반경 기본값 (1km/500m) | V3/V10 | 통일 여부 미정 |
| `searchOption`, `declared/measuredCategory` 의미 | courses.json | 용도 미정 |

---

## 변경 이력
- **v0.1 (2026-07-09)**: 확정 기능 초안 — 인증/프로필/지역/코스/러닝기록/장소. 미결 항목은 §7로 분리.
