# 업적(V14) 배지 에셋 · 메타데이터 — 백엔드 전달본

Figma `달여` 파일 **HiFi 페이지 / Section 3(획득) · Section 4(미획득)** 에서 REST API로 추출한 결과입니다.
배지 **21종 × 2상태(on/off) = 42장**.

생성 스크립트: `scripts/figma/achievements.mjs` (재생성 방법은 맨 아래)

---

## 1. 파일 구성

```
export/achievements/
├─ achievements.csv     # 테이블 시딩용 (UTF-8 + BOM, Excel에서 바로 열림)
├─ achievements.json    # 동일 데이터 JSON 배열 (camelCase 키)
├─ README.md            # 이 문서
└─ images/              # WebP 42장, 450×450, 투명 배경(알파 보존), @3x
   ├─ jjamppong_on.webp     # 획득 = 컬러
   ├─ jjamppong_off.webp    # 미획득 = 흑백
   └─ ...
```

- **포맷은 WebP(q85)** 입니다. 같은 이미지 PNG 기준 6.87MB → **1.16MB (83% 절감)**. 42장 전체가 1.2MB라 EC2 정적 서빙/전송 비용 부담이 없습니다. iOS 14+ / Android 4.2+ WebView 모두 지원하므로 앱 호환성 문제도 없습니다.
- PNG 원본이 필요하시면 `--png` 옵션으로 같이 뽑아 드릴 수 있습니다.
- 파일명 규칙: **`{code를 소문자로}_on.webp` / `_off.webp`** — code만 알면 파일명이 결정됩니다.
- on/off는 **같은 원본 이미지**이고, 미획득본은 디자인상 채도를 0으로 뺀 흑백입니다(스크립트가 두 이미지의 원본 동일성을 검증합니다).
- 원본 해상도는 1254×1254지만, 시안 표시 크기 150pt 기준 @3x인 450×450으로 뽑았습니다. 더 크게 필요하면 `--scale 4`(600×600)로 재추출 가능합니다.

## 2. CSV 컬럼

| 컬럼 | 설명 |
|---|---|
| `code` | **PK**. 업적 고정 코드. 현재 API(`GET /achievements`)의 `code`와 동일 규칙 |
| `category` | `GUNSAN` / `JEONJU` / `COMMON`. **`COMMON`은 지역 무관 업적(신규 개념)** |
| `sort_order` | 목록 기본 정렬값(군산 → 전주 → 공통, 10 단위) |
| `name` | 화면에 뜨는 업적 이름 |
| `description` | 화면에 뜨는 설명 |
| `unlock_condition` | **달성 판정 조건(백엔드 구현용).** 시안 설명문을 조건문으로 정리한 값 — 화면에는 안 띄웁니다 |
| `icon_on` / `icon_off` | 이미지 파일명 |

## 3. 전체 목록 (21종)

| 순서 | code | 분류 | 이름(화면) | 설명(화면) | 달성 조건 |
|---|---|---|---|---|---|
| 10 | `GUNSAN_SEONYUDO` | GUNSAN | 선유도 짱 | 군산의 선유도 해변 코스를 완주했다. | 군산 선유도 해변 코스 완주 |
| 20 | `GUNSAN_CONQUEROR` | GUNSAN | 군산 런트립 정복자 | 군산의 모든 추천코스를 완주했다. | 군산 추천 코스 전체 완주 |
| 30 | `JJAMPPONG` | GUNSAN | 짬뽕을 먹을 자격이 있는 자 | 군산의 짬뽕거리 코스를 완주했다. | 군산 짬뽕거리 코스 완주 |
| 40 | `GUNSAN_BEGINNER` | GUNSAN | 군산 초보 러너 | 군산의 러닝코스를 즐겨봤다. | 군산 지역 런트립 1회 완주 |
| 50 | `NATURE_LOVER` | GUNSAN | 자연을 사랑해! | 군산의 편백나무 숲 코스를 완주했다. | 군산 편백나무 숲 코스 완주 |
| 60 | `BETWEEN_WAVES` | GUNSAN | 부숴지는 파도들 사이에서 | 군산의 새만금 방파제 코스를 완주했다. | 군산 새만금 방파제 코스 완주 |
| 70 | `JEONJU_CHERRY_BLOSSOM` | JEONJU | 천변벚꽃 | 삼천변 벚꽃길 방문 | 전주 삼천변 벚꽃길 코스 완주 |
| 80 | `JEONJU_BEGINNER` | JEONJU | 전주 초보 러너 | 전주의 러닝코스를 즐겨봤다. | 전주 지역 런트립 1회 완주 |
| 90 | `JEONJU_DEOKJIN_LAKE` | JEONJU | 덕진 호수 | 덕진호수의 연꽃들 관람 | 전주 덕진호수 코스 완주 |
| 100 | `JEONJU_CONQUEROR` | JEONJU | 전주 런트립 정복자 | 전주의 모든 추천코스를 완주했다. | 전주 추천 코스 전체 완주 |
| 110 | `JEONJU_PILGRIM` | JEONJU | 전주 성지순례자 | 전주의 천주교 성지 코스를 완주했다. | 전주 천주교 성지 코스 완주 |
| 120 | `JEONJU_ECO_MUSEUM` | JEONJU | 전주 자연생태관 | 전주 자연생태관 방문 | 전주 자연생태관 코스 완주 |
| 130 | `LONG_RUN_3H` | COMMON | 장기간 러닝 성공 | 3시간 넘게 런트립 | 단일 런트립 러닝 시간 ≥ 3시간 |
| 140 | `FINISH_10` | COMMON | 완주 10회 달성 | 완주 10회 | 누적 완주 횟수 ≥ 10회 |
| 150 | `ICE_CREAM_RUNNER` | COMMON | 아이스크림 러너 | 12월 중 런트립 완주 | 12월(로컬 시각 기준) 런트립 1회 완주 |
| 160 | `DISTANCE_100KM` | COMMON | 100km 이상 | 누적거리 달성 | 누적 러닝 거리 ≥ 100km |
| 170 | `EARLY_BIRD` | COMMON | 얼리버드 | 아침 8시 이전에 런트립 시작 | 런트립 시작 시각 < 08:00 (로컬) |
| 180 | `WAYPOINT_3` | COMMON | 경유지 3개 지나감 | 경유지 3개 거치고 완주 | 경유지 3개 이상 코스 완주 |
| 190 | `REST_TIME` | COMMON | 휴식타임 | 카페, 음식점을 도착지로 설정하고 러닝 완주 | 도착지 카테고리가 카페/음식점인 코스 완주 |
| 200 | `SLOW_WALKER` | COMMON | 뚜벅이 | 완주시 페이스가 키로당 몇분 | 완주 평균 페이스 ≥ 10분/km ⚠️ **기준값 미확정** |
| 210 | `PIONEER` | COMMON | 개척자 | 새로운 코스를 만들어 런트립 | 직접 만든 코스로 런트립 완주 |

> 기존 API §8.1 고정 8종(`GUNSAN_BEGINNER` `JJAMPPONG` `GUNSAN_CONQUEROR` `NATURE_LOVER`
> `BETWEEN_WAVES` `JEONJU_BEGINNER` `JEONJU_CONQUEROR` `JEONJU_PILGRIM`)의 **코드는 그대로 유지**했습니다.
> 나머지 13종이 신규입니다.

## 4. 제안 테이블 (MySQL 기준 예시)

```sql
CREATE TABLE achievement (
  code             VARCHAR(40)  NOT NULL PRIMARY KEY,
  category         VARCHAR(16)  NOT NULL,           -- GUNSAN | JEONJU | COMMON
  sort_order       INT          NOT NULL,
  name             VARCHAR(60)  NOT NULL,           -- 화면 표시 이름
  description      VARCHAR(200) NOT NULL,           -- 화면 표시 설명
  unlock_condition VARCHAR(200) NOT NULL,           -- 판정 조건(내부용)
  icon_on_url      VARCHAR(300) NOT NULL,           -- 획득(컬러) 이미지 URL
  icon_off_url     VARCHAR(300) NOT NULL,           -- 미획득(흑백) 이미지 URL
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_achievement_category (category, sort_order)
);
```

`achievements.csv`를 그대로 `LOAD DATA` 하거나 시드 스크립트로 넣으시면 됩니다
(`icon_on`/`icon_off`는 파일명이므로 앞에 정적 호스팅 base URL을 붙여 `icon_*_url`로 저장).

## 5. 프론트 요청 — API 응답에 추가해 주셨으면 하는 필드

현재 `GET /achievements` 응답은 `code / name / description / unlocked / unlockedAt` 입니다. 여기에:

```json
{
  "code": "JJAMPPONG",
  "category": "GUNSAN",
  "name": "짬뽕을 먹을 자격이 있는 자",
  "description": "군산의 짬뽕거리 코스를 완주했다.",
  "iconOnUrl": "https://.../achievements/jjamppong_on.webp",
  "iconOffUrl": "https://.../achievements/jjamppong_off.webp",
  "sortOrder": 30,
  "unlocked": true,
  "unlockedAt": "2026-07-09T07:35:10Z"
}
```

- `category` — 화면 상단 지역 탭(군산/전주) 분기에 필요. **`COMMON`이 새로 생겨서** 기존 "코드 접두사로 지역 판정" 방식은 더 못 씁니다.
- `iconOnUrl` / `iconOffUrl` — 둘 다 내려주시면 프론트가 `unlocked` 여부로 골라 씁니다. (한 개만 내리고 프론트에서 CSS 흑백 필터를 쓰는 방식도 가능하지만, 시안의 흑백 처리가 단순 grayscale이 아니라서 **두 장 다 받는 쪽을 권장**합니다.)
- `unlock_condition`은 화면에 안 띄우므로 응답에 넣지 않아도 됩니다.

## 6. 확인 필요 (기획/디자인)

1. **`SLOW_WALKER`(뚜벅이)** — 시안 설명이 "완주시 페이스가 키로당 몇분"으로 숫자가 비어 있습니다. 기준 페이스 확정 필요. 일단 `10분/km 이상`으로 가정해 두었습니다.
2. **`DISTANCE_100KM`** — "누적거리 달성"이 개인 누적 총거리 100km인지 확인 필요(그렇게 가정).
3. **화면 문구 오타** — 시안의 `졍유지` → `경유지`로 고쳐서 넣었습니다. `부숴지는 파도들 사이에서`는 맞춤법상 `부서지는`이 맞지만 배지 이름이라 시안 그대로 두었습니다. 수정 원하시면 알려주세요.
4. **기존 mock과 다른 문구** — 프론트 mock에 있던 `부숴지는 파도를 사이에서`는 시안 기준(`파도들`)으로 맞추겠습니다.

## 7. 재생성 방법

```bash
node scripts/figma/fetch.mjs --force        # Figma 파일 캐시 갱신
node scripts/figma/achievements.mjs         # 목록 + 매칭 검증만 (저장 안 함)
node scripts/figma/achievements.mjs --write # WebP + CSV + JSON 저장
node scripts/figma/achievements.mjs --write --png       # PNG 원본도 같이
node scripts/figma/achievements.mjs --write --scale 4   # 600×600로
node scripts/figma/achievements.mjs --write --quality 90 # WebP 품질 조정
```

시안에 배지가 추가되면 `scripts/figma/achievements.mjs`의 `REGISTRY`에 카드 프레임 이름 → code를 한 줄 추가하면 됩니다. 등록 안 된 카드가 있으면 스크립트가 경고하고 저장을 중단합니다.
