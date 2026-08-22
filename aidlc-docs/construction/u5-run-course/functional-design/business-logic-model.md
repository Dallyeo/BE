# U5 Business Logic Model — 러닝기록 🔒

> 기술 중립 비즈니스 흐름. 컴포넌트: `RunController/RunService/RunRepository`.
> **범위 변경(2026-08-22)**: 사용자 코스 생성(US-COURSE-3) 백엔드 제거 → `CourseCommandService`/`POST /courses` 없음. Course(U2)는 읽기 참조만.
> 모든 엔드포인트 🔒 — `@AuthUser Long userId` 주입(U4). 미인증 → 401(SecurityConfig deny-by-default).

---

## 컴포넌트 개요

| 컴포넌트 | 책임 |
|---|---|
| `RunController` | `/runs` 엔드포인트(저장/목록/상세). 요청 파싱·검증 위임·응답 래핑 |
| `RunService` | 러닝 기록 저장/조회 비즈니스 로직, 소유권 검증, courseName lookup |
| `RunRepository` | Run 영속. 본인 기록 조회(기간 필터, 정렬) 쿼리 |
| `CourseRepository` (U2 재사용) | courseName lookup 전용(읽기). 변경 없음 |

---

## 흐름 1. 러닝 기록 저장 — US-RUN-1 (`POST /runs`)

```
1. @AuthUser로 userId 확보 (미인증 → 401)
2. 요청 검증 (business-rules BR-U5-5):
   - polyline 비어있지 않음
   - distanceMeters > 0, durationSeconds > 0
   - startedAt, finishedAt 존재 && finishedAt >= startedAt
   - averagePaceSeconds 존재
   위반 → 400 (BusinessException BAD_REQUEST)
3. Run 엔티티 구성:
   - userId = 현재 사용자
   - courseId = 요청 courseId (nullable, 존재검증 안 함 — Q7=A)
   - polyline/distance/duration/averagePace/startedAt/finishedAt = 요청 값
   - createdAt = now (@PrePersist)
   - (completionRate 계산·저장 안 함 — Q6=A)
4. RunRepository.save → id 생성
5. 응답 조립(RunDetailResponse): id, courseId, distanceMeters, durationSeconds,
   averagePaceSeconds, startedAt, finishedAt (+ completionRate: null)
6. ApiResponse.success(...) 201
```

**엣지**: courseId가 null → 자유 러닝 또는 직접 만든 경로(정상). courseId가 존재하지 않는 코스 → 그대로 저장(느슨).

---

## 흐름 2. 러닝 기록 목록 조회 — US-RUN-2 (`GET /runs?from&to`)

```
1. @AuthUser로 userId 확보
2. from/to(ISO date, optional) 파싱 → Instant 범위로 변환
   - from만/to만/둘 다/둘 다 없음 모두 허용
   - 파싱 실패 → 400
3. RunRepository 조회: userId == 현재 사용자 AND
   (from<=finishedAt<=to 범위), finishedAt DESC (Q8=A)
4. 각 Run → RunSummaryResponse 매핑:
   - id, distanceMeters, durationSeconds, finishedAt
   - courseName: courseId != null 이면 CourseRepository lookup (best-effort;
     코스 없거나 시드에 없으면 null). courseId == null → null (자유 러닝)
5. ApiResponse.success(List<RunSummaryResponse>) 200
```

**본인 격리**: 쿼리에서 userId로 필터 → 타인 기록은 애초에 결과에 포함되지 않음.

---

## 흐름 3. 러닝 기록 상세 조회 — US-RUN-3 (`GET /runs/{id}`)

```
1. @AuthUser로 userId 확보
2. RunRepository.findById(id)
   - 없음 → 404 (NOT_FOUND)
3. 소유권 검증: run.userId == 현재 사용자?
   - 아니면 → 404 (business-rules BR-U5-2, 존재 은폐)
4. RunDetailResponse 조립 (목록 요약 + polyline 포함)
   - courseName: courseId lookup(best-effort)
5. ApiResponse.success(...) 200
```

---

## 데이터 흐름 요약

```
POST /runs      → 검증 → Run 저장(userId 귀속) → 201
GET  /runs      → userId 필터 + 기간 + 최신순 → courseName lookup → 200
GET  /runs/{id} → 소유권 검증 → 상세(polyline 포함) → 200/404
```

> 코스 조회(`GET /courses`, `GET /courses/{id}`)는 U2 그대로. U5에서 신규/변경 없음.
