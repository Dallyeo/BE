# U6 업적 (Achievement) 🔒 — Functional Design (compact)

> 출처: `src/main/resources/data/img_1.png`(업적 8종). 러닝 기록(U5)에 연동되는 신규 도메인.
> 결정: Q1=둘 다(자동+수동) · Q2=courseId 기반 · Q3=전체+달성여부.

---

## 도메인 모델

### 업적 카탈로그 — `AchievementType` (enum, 고정 8종)
조건 로직이 데이터에 결합돼 있어 seed 테이블이 아닌 enum으로 관리. 각 항목: `code`(enum명)/`displayName`/`description`/`kind`/`region`/`courseId`.

| code | 업적명 | kind | 파라미터 |
|---|---|---|---|
| GUNSAN_BEGINNER | 군산 초보 러너 | REGION_ANY | 군산 |
| JJAMPPONG | 짬뽕을 먹을 자격이 있는 자 | COURSE | gunsan-jjamppong-run |
| GUNSAN_CONQUEROR | 군산 런트립 정복자 | REGION_ALL | 군산 |
| JEONJU_BEGINNER | 전주 초보 러너 | REGION_ANY | 전주 |
| JEONJU_CONQUEROR | 전주 런트립 정복자 | REGION_ALL | 전주 |
| JEONJU_PILGRIM | 전주 성지순례자 | COURSE | jeonju-catholic-shrine-run |
| NATURE_LOVER | 자연을 사랑해! | COURSE | gunsan-cypress-forest-run |
| BETWEEN_WAVES | 부숴지는 파도를 사이에서 | COURSE | gunsan-saemangeum-run |

### 달성 기록 — `UserAchievement` (엔티티, 신규 테이블 `user_achievement`)
`id(Long)`, `userId(Long)`, `achievement(AchievementType, STRING)`, `unlockedAt(Instant, @PrePersist)`. 유니크 `(userId, achievement)`.

---

## 판정 로직 (Q2=courseId 기반)
사용자가 저장한 run의 **distinct courseId 집합**(자유 러닝 null 제외)으로 판정. 지역별 코스 집합은 `course` 테이블 실데이터로 해석(코스 추가 시 자동 반영).

| kind | 충족 조건 |
|---|---|
| COURSE | userCourseIds ∋ courseId |
| REGION_ANY | 해당 지역 코스 중 하나 이상 ∈ userCourseIds |
| REGION_ALL | 해당 지역 코스 전부 ⊆ userCourseIds (지역 코스 비어있지 않을 때) |

## 업무 규칙
- **BR-U6-1 (인증)**: 모든 업적 API 🔒. 미인증 → 401.
- **BR-U6-2 (자동 달성)**: `POST /runs` 저장 시 `evaluateAndUnlock(userId)` 호출 → 충족·미달성 업적 자동 저장. run 응답 형태는 U5 그대로(비침습).
- **BR-U6-3 (수동 달성)**: `POST /achievements/{code}/unlock` — 조건 재판정. 미지원 code → 404, 조건 미충족 → 409, 이미 달성 → 기존 응답(멱등, 저장 안 함).
- **BR-U6-4 (목록)**: `GET /achievements` — 전체 8종 + 본인 달성여부/일시. 본인 격리(userId).
- **BR-U6-5 (중복 방지)**: `(userId, achievement)` 유니크 + 저장 전 존재 확인.
- **BR-U6-6 (범위)**: 업적 해제/취소, 진행률(%) 표시, 신규 업적 추가 UI는 범위 밖(백로그).

## API
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/achievements` 🔒 | 전체 업적 + 달성여부 |
| POST | `/achievements/{code}/unlock` 🔒 | 특정 업적 수동 달성(조건 재판정) |
| (내부) | `POST /runs` 훅 | 러닝 저장 시 자동 판정 |
