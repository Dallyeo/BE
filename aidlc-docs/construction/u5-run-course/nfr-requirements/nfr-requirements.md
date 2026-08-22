# NFR Requirements — U5 러닝기록 🔒 (light)

> 확정: NQ1=A(polyline 상한 없음/defer, 단 컬럼 MEDIUMTEXT) · NQ2=A(페이징 없음/defer, 목록 polyline 제외) · NQ3=신규 기술스택 없음.
> U1-a/U2/U4 NFR 전면 상속. run 도메인은 인증된 소유자 전용 CRUD(저장/목록/상세)로 신규 외부호출·시크릿 없음.

---

## 1. 확장성
- 단일 인스턴스(단일 EC2 코로케이션, app+MySQL+Redis) 상속. run은 stateless 요청(토큰 기반) → 앱 수평확장 무관.
- 초기 규모 소규모. 사용자별 기록 수 증가는 `(userId, finishedAt)` 인덱스로 흡수. 대량화 시 목록 **페이징 백로그**(NQ2).

## 2. 성능 (N2 기조 상속 — 하드 타깃 미설정)
- 저장/조회 모두 로컬 MySQL 단일 트랜잭션. 외부 API 호출 없음(TourAPI/소셜 무관).
- **목록 조회**: `(userId, finishedAt)` 복합 인덱스로 본인 기록 기간 필터·최신순 정렬. 요약 응답은 **polyline 제외**(경량 payload, NQ2).
- **상세/저장**: polyline(JSON TEXT) 직렬화/역직렬화 1회. courseName은 목록 시 시드 코스 best-effort lookup(소량).
- 하드 응답시간 타깃 미설정. 관측만(로깅).

## 3. 가용성·신뢰성
- 외부 의존 없음 → U4 대비 장애면(소셜/JWKS) 부재. DB 가용성에만 의존(U1-a 상속).
- **데이터 무결성**: polyline 상한 미설정(NQ1). `TEXT`(≈64KB)는 장거리 러닝에서 절단 위험 → **컬럼 타입 MEDIUMTEXT**로 지정(≈16MB, 저비용 안전장치). NFR Design/Code에 반영.
- 저장 실패(DB 오류) 시 공통 예외 → 500 일관 응답. 부분 저장 없음(단일 엔티티).

## 4. 보안 [Security Baseline]
| 규칙 | U5 반영 | 상태 |
|---|---|---|
| SECURITY-05 (입력 검증) | polyline 비어있지않음, distance/duration>0, finishedAt>=startedAt, from/to 파싱 검증 → 위반 400 (BR-U5-5/9) | 준수 |
| SECURITY-08 (deny-by-default 접근제어) | `/runs/**` 전부 authenticated(U1-b SecurityConfig 상속). 화이트리스트 추가 없음 | 준수 |
| 리소스 소유권(A01 Broken Access Control) | 목록=userId 쿼리 격리, 상세=소유권 검증 후 타인 **404 은폐**(BR-U5-2) | 준수 |
| SECURITY-03 (로깅·민감정보) | 구조화 로깅 상속. polyline 좌표(위치=민감) **본문 미로깅**, 식별자/집계값만 | 준수 |
| SECURITY-15 (예외/페일세이프) | GlobalExceptionHandler 상속, 안전 응답 | 준수 |
| SECURITY-01 (at-rest/in-transit) | in-transit nginx TLS 상속. at-rest 단일 EC2 로컬디스크 = U4와 동일 **accepted risk**. **위치정보(polyline) 저장**은 개인정보 → RDS 이관 시 암호화 우선 대상으로 명시 | 문서화된 수용 |
| SECURITY-12 (인증) | JWT 자체발급·검증(U4) 상속. run은 신규 credential 없음 | 상속 |
| 시크릿 관리 | run 신규 시크릿 없음 | N/A |

## 5. 유지보수·관측성
- 구조화 로깅 상속. 관측 지점(민감정보 제외): 러닝 저장 성공(userId·distanceMeters·courseId 유무), 검증 실패(400) 카운트. **polyline 좌표는 미로깅**(위치 프라이버시).
- 소유권 위반(404) 이벤트 관측(비정상 접근 탐지 힌트).

## 6. N/A / defer (현 단계)
- 오토스케일링 / DR·failover / 컴플라이언스 — N/A(상속).
- HTTP 보안 헤더(SECURITY-04): API-only(JSON) → N/A.
- 레이트리밋 — defer(U4와 동일 기조).
- 목록 페이징 / polyline 크기 상한 — defer(NQ1/NQ2, 백로그).
- 통계/완주율 성능 — 범위 밖(완주율 미계산).
