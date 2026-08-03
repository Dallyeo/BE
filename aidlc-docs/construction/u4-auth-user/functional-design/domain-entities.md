# U4 — Domain Entities (인증·사용자)

> 기술 비의존 도메인 모델. auth 도메인은 상태를 DB에 두지 않고(토큰은 stateless JWT + Redis refresh), **User** 가 유일한 영속 엔티티.
> 결정 반영: Q3=A(provider+providerUserId 유니크) · Q5=A(HS256) · Q6=A(1세션/회전) · Q7=A(하드 삭제) · Q8=B(onboardingCompleted 플래그).

---

## 1. User (영속 엔티티 · MySQL `users`)

| 필드 | 타입 | 제약/기본값 | 설명 |
|---|---|---|---|
| `id` | Long (PK) | auto increment | 내부 사용자 식별자. JWT `sub`. |
| `provider` | Enum `Provider` | not null | `KAKAO` \| `APPLE` |
| `providerUserId` | String | not null | 소셜 고유 ID(카카오 회원번호 / Apple `sub`). |
| `nickname` | String | not null | 소셜 닉네임 또는 자동생성(`러너####`). 이후 수정 가능. |
| `gender` | Enum `Gender` | not null, default `NONE` | `MALE` \| `FEMALE` \| `NONE` |
| `height` | Double | nullable | cm. 온보딩 미입력 시 null. |
| `weight` | Double | nullable | kg. 온보딩 미입력 시 null. |
| `profileImageUrl` | String | nullable (**보류**) | 저장방식 미정 → 항상 null 반환, 이번 유닛 미구현. |
| `onboardingCompleted` | boolean | not null, default `false` | Q8=B. 온보딩 화면 완료(저장/건너뛰기) 시 true. |
| `createdAt` | Instant | not null, auto | 가입 시각. |
| `updatedAt` | Instant | not null, auto | 최종 수정 시각. |

**유니크 제약**: `(provider, providerUserId)` 복합 유니크 — 재로그인 매칭 키.
**삭제 정책**: 하드 삭제(Q7=A). 소프트 삭제 컬럼 없음. `DELETE /users/me` 시 레코드 물리 삭제.

### Enum: Provider
- `KAKAO` — 실제 구현
- `APPLE` — 실제 구현 (Q1=B, 둘 다 실제 연동)

### Enum: Gender
- `MALE`, `FEMALE`, `NONE`(미지정/기본)

---

## 2. RefreshToken (Redis · 비영속 세션 상태)

> 별도 JPA 엔티티 아님. Redis 키-값으로 관리. Q6=A(사용자당 1세션 + 회전).

| 항목 | 값 |
|---|---|
| 키 | `refresh:{userId}` |
| 값 | 현재 유효한 Refresh Token 문자열(또는 해시) — **회전 시 교체** |
| TTL | 7일(604800초) — Refresh Token 만료와 동일 |
| 무효화 | 로그아웃/계정삭제 시 키 삭제, 갱신 시 새 값으로 덮어씀(이전 토큰 자동 무효) |

- 한 사용자당 항상 **최대 1개**의 유효 Refresh Token → 신규 기기 로그인 시 이전 기기 refresh는 무효(단일 세션 가정).
- Access Token은 **저장하지 않음**(stateless 검증). 만료 전까지 유효.

---

## 3. 토큰 모델 (JWT · stateless)

> Q5=A: HS256 대칭키(env `JWT_SECRET`), 자체 발급·검증만 수행.

| 클레임 | Access | Refresh | 설명 |
|---|---|---|---|
| `sub` | userId | userId | 사용자 식별자 |
| `type` | `"access"` | `"refresh"` | 토큰 용도 구분(교차 사용 방지) |
| `iat` | 발급시각 | 발급시각 | |
| `exp` | iat + 24h | iat + 7d | 만료 |

- 서명: HS256, 시크릿은 환경변수 `JWT_SECRET`(기존 U1-a `.env` 패턴 계승).
- 검증 실패(서명 불일치/만료/type 불일치) → 인증 실패.

---

## 4. 관계 요약
```
User (1) ──(provider+providerUserId 유니크)── 소셜 계정 1개
User (1) ──(Redis refresh:{userId})── RefreshToken 최대 1개
User (1) ──(향후 U5)── Run(N), Course(N)   ← 이번 유닛 범위 밖
```
- 이번 유닛에서 User는 다른 엔티티와 FK 없음(runs/courses는 U5). 하드 삭제 단순.

---

## 5. OAuthUser (도메인 값 객체 · 비영속)
소셜 검증 결과를 담는 임시 객체(OAuthClient 반환).

| 필드 | 설명 |
|---|---|
| `provider` | KAKAO/APPLE |
| `providerUserId` | 소셜 고유 ID |
| `nickname` | 소셜 프로필 닉네임(없으면 null → 자동생성 대상) |

> 이메일·프로필사진 등은 수집하지 않음(Q3=A, 최소 수집).
