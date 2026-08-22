# U5 러닝기록 🔒 — Functional Design Plan

> **유닛**: U5 (run 도메인만)
> **스토리**: US-RUN-1(기록 저장), US-RUN-2(기록 목록), US-RUN-3(기록 상세)
> **범위 변경(2026-08-22, 사용자 결정)**: ~~US-COURSE-3(사용자 코스 생성)~~ → 백엔드에서 제거. 사용자 코스는 DB 저장하지 않음(프론트/클라이언트 책임). 따라서 Q2~Q5(코스 생성 관련)는 무효, 아래 답변에 취소선 표기. Course(U2) 엔티티 변경 없음.
> **선행/의존**: U4(JWT 토큰·`@AuthUser` 주입, 소유권 검증 전제), U2(Course 조회·CourseRepository 읽기 참조만 재사용)
> **관례 재사용**: `ApiResponse<T>`, `BusinessException`+`ErrorCode`, `@AuthUser Long userId` 파라미터 주입, JSON `@Convert` TEXT 컬럼(Polyline 등)

---

## 실행 체크리스트 (Functional Design)

- [x] Step 1: 유닛 컨텍스트 분석 (unit-of-work U5, story-map, api-spec §4.3/§5) — 완료
- [x] Step 2: 본 계획 + 질문 작성 — 진행 중
- [x] Step 3: 사용자 답변 수집 및 모호성 해소
- [x] Step 4: `domain-entities.md` 작성 (Run 엔티티) — Course 확장 제거(범위 변경)
- [x] Step 5: `business-logic-model.md` 작성 (기록 저장/조회) — 코스 생성 흐름 제거(범위 변경)
- [x] Step 6: `business-rules.md` 작성 (소유권·검증·완주율 규칙)
- [ ] Step 7: 완료 메시지 및 승인 대기

---

## 설계 질문 (아래 각 `[Answer]:` 뒤에 A~E 중 선택 또는 자유 기술)

CLAUDE.md 규칙상 모호성 최소화를 위해 사전 결정이 필요한 항목입니다. 기본 추천안을 **A**에 배치했습니다.

---

### Q1. Run(러닝 기록) 식별자 타입
러닝 기록은 사용자가 생성하는 레코드입니다. api-spec에는 `"id": "long"`으로 표기됨.
- A. `Long` + `@GeneratedValue(IDENTITY)` (User 엔티티와 동일 관례, DB auto-increment) — **추천**
- B. `String` UUID
- C. 기타

[Answer]: A (Long + @GeneratedValue IDENTITY)

---

### Q2. 사용자 생성 코스(US-COURSE-3)의 식별자
기존 시드 코스는 `String id`(예: `gunsan-modern-history-run`). 사용자 코스는 id를 서버가 생성해야 함.
- A. `String` UUID를 서버가 생성 (예: `user-<uuid>`) — 기존 Course.id 타입(String) 유지, 시드와 구분 접두어 — **추천**
- B. `String` 순수 UUID (접두어 없음)
- C. Course.id를 Long으로 마이그레이션 (시드 포함 전면 변경) — 영향 큼, 비추천
- D. 기타

[Answer]: ~~A~~ → 무효(코스 생성 백엔드 제거)