# Frontend API Contract — Dallyeo

> **목적**: 프론트엔드 개발자가 **이미 구현해 둔 기능**에 백엔드를 맞추기 위한 API 명세.
> 프론트가 실제로 호출하는 엔드포인트만 아래에 채워주세요. (없는 기능은 넣지 않습니다)
>
> **작성 방법**: 아래 "엔드포인트 작성 템플릿"을 복사해서, 프론트가 실제 쓰는 API 개수만큼 추가하세요.
> 도메인 구분(인증/코스/러닝/추천 등)은 편하게 붙이거나 생략하셔도 됩니다.

---

## 작성 규칙 / 범례
- **인증**: 🔒 토큰 필요 / 🌐 공개(public, 토큰 불필요)
- **응답**: 성공 시 HTTP status와 프론트가 기대하는 실제 JSON을 그대로 적어주세요
- 응답 데이터가 없으면 `204 No Content`로 표기
- 프론트가 실제로 호출하는 **URL·메서드·요청 바디·응답 JSON**을 그대로 붙여주시면 정확히 맞출 수 있습니다

### 📋 엔드포인트 작성 템플릿 (복사해서 사용)
```
### [기능 이름]
- **Method / Path**: `GET /api/...`
- **인증**: 🔒 / 🌐
- **기능 설명**: (이 API가 하는 일)
- **요청 (Request)**:
  - Path/Query 파라미터: 
  - Request Body (JSON):
    ```json
    { }
    ```
- **응답 (Response)**:
  - 성공 status: 200 / 201 / 204
  - Response Body (JSON):
    ```json
    { }
    ```
- **비고**: 
```

---

## 엔드포인트 목록 (프론트가 실제 만든 것만)

<!-- 여기에 위 템플릿을 복사해서 실제 엔드포인트를 추가하세요 -->
### [장소 검색] 
- **Method / Path**: `GET /places/search?keyword={k}&regionCode={code}`
- **인증**: 🌐
- **기능 설명**: (V04 검색/V05 검색 결과에서 사용할 프론트엔드가 만든 api)
- **요청 (Request)**:
  - Path/Query 파라미터:
  - Request Body (JSON):
    ```json
    {
      "id": "...",
      "name":"이름",
      "category": "카테고리",
      "latitude": "좌표lat",
      "longitude": "좌표lon",
      "address": "주소",
      "businessHours": "00:00-00:00", //영업시간
      "imageUrl": "사진url"
    }
    ```
- **응답 (Response)**:
  - 성공 status: 200 / 201 / 204
  - Response Body (JSON):
    ```json
    {
      "id": "String",
      "name": "String",
      "category": "enum-String",
      "latitude": "double",
      "longitude": "double",
      "address": "String",
      "thumbnailUrl": "String"
     }
    ```

### [장소 상세]
- **Method / Path**: `GET /places/{id}`
- **인증**: 🌐
- **기능 설명**: (V04 검색/V05 검색 결과에서 사용할 프론트엔드가 만든 api)
- **요청 (Request)**:
  - Path/Query 파라미터:
  - Request Body (JSON):
    ```json
    {
      "id": "...",
      "name":"이름",
      "category": "카테고리",
      "latitude": "좌표lat",
      "longitude": "좌표lon",
      "address": "주소",
      "businessHours": "00:00-00:00", //영업시간
      "imageUrl": "사진url"
    }
    ```
- **응답 (Response)**:
  - 성공 status: 200 / 201 / 204
  - Response Body (JSON):
    ```json
    {
      "id": "String",
      "name": "String",
      "category": "enum-String",
      "latitude": "double",
      "longitude": "double",
      "address": "String",
      "thumbnailUrl": "String"
     }
    ```
- **비고**:


---

## 공통 응답 래퍼 형식 (있다면)
> 프론트가 기대하는 성공/실패 공통 응답 구조가 있으면 적어주세요.

- **성공 응답 예시**:
  ```json
  { }
  ```
- **실패 응답 예시**:
  ```json
  { }
  ```

---

## 📎 고정 코스 폴리라인 데이터 (JSON) — 확인 완료

- **파일 위치**: `aidlc-docs/inception/requirements/data/courses/courses.json`
- **총 코스 수**: 10개 (군산 6, 전주 4)

### 데이터 구조 (코스 1개당)
| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | string (slug) | 코스 식별자 (예: `gunsan-modern-history-run`) |
| `name` | string | 코스 이름 (예: "근대 역사 박물관 런") |
| `region` | string | 지역 (`군산` / `전주`) |
| `searchOption` | int | 용도 미확정 (현재 전부 `0`) — ❓회의 필요 |
| `declaredCategory` | string | 선언 거리 분류 (`단거리`/`중거리`/`장거리`) |
| `measuredCategory` | string | 측정 거리 분류 (`단거리`/`중거리`/`장거리`) |
| `polyline` | `[{lat, lng}]` | 경로 좌표 배열 (코스당 111~660개) |
| `cumulativeMeters` | `[int]` | 각 좌표까지 누적 거리(m), polyline과 길이 동일 |
| `totalMeters` | int | 총 거리(m) |
| `waypointAnchors` | `[{name, polylineIndex}]` | 경유지 앵커 (이름 + polyline 인덱스) |

### 백엔드 저장 방식 (제안 — 확정 전 검토용)
- **Course 테이블(스칼라)**: `id`, `name`, `region`, `declaredCategory`, `measuredCategory`, `totalMeters`, `searchOption`
- **경로 데이터**: `polyline` / `cumulativeMeters` / `waypointAnchors`는 **MySQL JSON 컬럼**에 원본 형태 그대로 저장 권장
  - 이유: (1) 읽기 위주 + 프론트에 JSON 그대로 내려주면 됨, (2) 좌표를 행(row)으로 쪼개면 코스당 수백 행 × 불필요한 조인 발생, (3) 코스 폴리라인 자체는 근접 검색 대상이 아님(근접 검색은 '장소' 쪽)
  - 대안: 좌표를 별도 `course_point` 테이블로 정규화 → 지금 요구사항엔 과함
- **`region`**: V2 "지역별 추천 코스 / 지역 데이터"에 쓰이므로 별도 지역(enum 또는 코드) 개념으로 관리 권장
- **사용자 생성 코스(V7)**: 프론트가 Tmap으로 만든 경로 JSON을 동일 구조로 받아 저장 (백엔드는 Tmap 호출 안 함 — 기존 결정과 일치)

### ❓ 확인 필요 (회의 안건)
- `searchOption`의 의미/용도 (현재 전부 0)
- `declaredCategory` vs `measuredCategory` 차이와 각각의 용도
- 거리 분류(단/중/장) 기준 경계값(m)
- 사용자 생성 코스도 `cumulativeMeters`/`waypointAnchors`를 프론트가 채워 보내는지
