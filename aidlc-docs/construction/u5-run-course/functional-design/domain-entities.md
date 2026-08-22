# U5 Domain Entities — 러닝기록 🔒

> 기술 중립 도메인 모델. 구현 상세(JPA 컬럼 타입 등)는 참고용으로만 병기.
> **범위 변경(2026-08-22)**: 사용자 코스 생성(US-COURSE-3)은 **백엔드 저장 대상 아님** → 코스 생성 API/Course 확장 없음. U5는 **run 도메인만**.
> 관례: JSON 구조(polyline 등)는 U2와 동일하게 JSON TEXT 컬럼(`@Convert`)로 저장.

---

## 1. Run (러닝 기록) — 신규 엔티티

사용자가 완료한 러닝의 스냅샷. 실시간 추적 없음 — 클라이언트가 추적을 끝낸 뒤 **완료 데이터**를 한 번 저장.
사용자가 프론트에서 직접 만든 경로로 달렸더라도, 그 경로는 별도 코스로 저장되지 않고 **이 Run의 `polyline`에만 남는다**.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | Long | 서버생성 | PK, `@GeneratedValue(IDENTITY)` (Q1=A) |
| `userId` | Long | 필수 | 소유자(= 토큰 사용자). FK 제약 없이 스칼라 저장, 소유권 검증 키 |
| `courseId` | String | nullable | 달린 **공식(시드) 코스** 참조(자유 러닝/직접 만든 경로면 null). **FK 제약 없음 / 존재검증 없음**(Q7=A) |
| `polyline` | List\<PolylinePoint\> | 필수(비어있지 않음) | 실제 달린 경로 좌표. JSON TEXT 컬럼(U2 `PolylineConverter` 재사용) |
| `distanceMeters` | int | 필수(>0) | 총 이동 거리(m) |
| `durationSeconds` | int | 필수(>0) | 총 소요 시간(초) |
| `averagePaceSeconds` | int | 필수 | 평균 페이스(초/km). **클라이언트 값 그대로 저장**(Q9=A) |
| `startedAt` | Instant | 필수 | 러닝 시작 시각(UTC) |
| `finishedAt` | Instant | 필수 | 러닝 종료 시각(UTC). `finishedAt >= startedAt` |
| `createdAt` | Instant | 서버생성 | 저장 시각(`@PrePersist`) |

**비고**
- `completionRate`(완주율)는 **엔티티에 저장하지 않음**(Q6=A). 계산 기준이 보류 백로그이므로, 필요한 원천(courseId, distanceMeters)만 보관하고 추후 계산.
- 소유권 모델: `userId` 스칼라 저장. `@ManyToOne User` 연관은 사용하지 않음(다른 도메인 관례와 일관 — courseId도 스칼라).
- 인덱스: `(userId, finishedAt)` 복합 인덱스 — 목록 조회(본인 기록, 기간 필터, 최신순)에 사용.

### PolylinePoint (재사용, U2 정의)
`{ lat: double, lng: double }` — `com.ppip.dallyeo.course.dto.PolylinePoint` 재사용.

---

## 2. Course — 변경 없음

- `Course` 엔티티(U2, 시드 코스)는 **이번 유닛에서 변경하지 않는다**. `ownerId` 등 추가 필드 없음.
- 사용자 생성 코스를 저장하지 않으므로 `CourseCommandService`/`POST /courses`도 만들지 않는다.
- `GET /courses`, `GET /courses/{id}`는 U2의 기존 공개 조회 그대로 유지(시드 코스 전용).
- Run은 시드 코스를 `courseId`(String, nullable)로 느슨하게 참조만 한다.

---

## 3. 엔티티 관계도 (텍스트)

```
User (U4)
  | 1
  | owns (userId 스칼라 참조, FK 제약 없음)
  | *
Run  --- courseId (String, nullable, FK 제약 없음) ---> Course (0..1, U2 시드 전용, 읽기만)
```

- Run → User: `userId` 스칼라(소유권)
- Run → Course: `courseId` 스칼라, optional, 느슨(존재하지 않아도 무방; 시드 코스 참조용)
