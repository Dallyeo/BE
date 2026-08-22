# U5 Business Rules — 러닝기록 🔒

> 규칙 ID 접두어 `BR-U5-`. 오류는 공통 `BusinessException` + `ErrorCode`(U1-a) 사용.
> 보안 원칙: deny-by-default(U1-b). 🔒 엔드포인트는 유효 토큰 필수, 소유 리소스는 소유자만.
> **범위 변경(2026-08-22)**: 사용자 코스 생성(US-COURSE-3) 백엔드 제거 → 관련 규칙(코스 소유권/생성 검증) 삭제.

---

## 인증·소유권

### BR-U5-1. 인증 필수 (🔒)
`POST /runs`, `GET /runs`, `GET /runs/{id}`는 유효한 액세스 토큰 필수.
- 미인증/무효 토큰 → **401 UNAUTHORIZED** (SecurityConfig, U1-b가 처리)
- 컨트롤러는 `@AuthUser Long userId`로 사용자 식별.

### BR-U5-2. 러닝 기록 소유권
러닝 기록은 저장한 사용자 본인만 조회 가능.
- 목록(`GET /runs`)은 쿼리에서 `userId = 현재 사용자`로 필터 → 타인 기록 미포함.
- 상세(`GET /runs/{id}`)에서 `run.userId != 현재 사용자` → **404 NOT_FOUND**(존재 은폐. 스토리 "403/404" 중 정보 노출 최소화를 위해 404 채택).
- 존재하지 않는 id → **404 NOT_FOUND**.

---

## 러닝 기록 저장 검증 — US-RUN-1 / `POST /runs`

### BR-U5-5. 필수 필드/값 검증
다음 위반 시 **400 BAD_REQUEST**:
- `polyline` 누락 또는 빈 배열
- `distanceMeters` 누락 또는 <= 0
- `durationSeconds` 누락 또는 <= 0
- `averagePaceSeconds` 누락
- `startedAt` 또는 `finishedAt` 누락
- `finishedAt < startedAt` (시간 역전)

### BR-U5-6. 코스 참조 느슨성
`courseId`는 optional(시드 코스 참조용).
- null → 자유 러닝 또는 직접 만든 경로(정상 저장).
- 값이 있으면 존재 여부를 **검증하지 않고** 그대로 저장(Q7=A). 조회 시 courseName은 best-effort lookup(코스 없으면 null).

### BR-U5-7. 평균 페이스 출처
`averagePaceSeconds`는 클라이언트 제공 값을 그대로 저장(서버 재계산 안 함, Q9=A).

### BR-U5-8. 완주율 보류
`completionRate`는 이번 유닛에서 계산·저장하지 않음(Q6=A). 응답에서는 null(또는 생략). 계산 기준 확정 시 백로그로 추가.

---

## 러닝 기록 목록 — US-RUN-2 / `GET /runs`

### BR-U5-9. 기간 필터/정렬
- `from`/`to`(ISO date)는 optional. 각각 독립적으로 생략 가능.
- 필터 기준 필드 = `finishedAt`, 정렬 = `finishedAt` 내림차순(최신순)(Q8=A).
- from/to 파싱 실패 → 400.
- 결과 없음 → 빈 배열 + 200.

---

## 범위 규칙

### BR-U5-12. 범위 한정
U5는 **US-RUN-1/2/3(러닝 기록 저장/목록/상세)만** 구현.
- **사용자 코스 직접 생성(US-COURSE-3)은 백엔드 범위 밖** — 코스 생성/저장은 프론트/클라이언트 책임. 사용자가 만든 경로는 러닝 기록의 `polyline`으로만 남음.
- 러닝 기록 **수정·삭제, 완주율 계산, 통계/집계**도 범위 밖(백로그)(Q11=A).

---

## 오류 코드 매핑 요약

| 상황 | HTTP | ErrorCode |
|---|---|---|
| 미인증/무효 토큰 | 401 | UNAUTHORIZED |
| 러닝 저장 필드/값 검증 실패 | 400 | BAD_REQUEST |
| from/to 파싱 실패 | 400 | BAD_REQUEST |
| 러닝 기록 없음/타인 | 404 | NOT_FOUND |
