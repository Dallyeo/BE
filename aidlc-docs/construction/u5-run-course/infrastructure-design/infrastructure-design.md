# Infrastructure Design (Light) — U5 러닝기록 🔒

> 논리 컴포넌트(`nfr-design/logical-components.md`)를 인프라에 매핑. U1-a~U4 인프라 전면 상속.
> **핵심**: 신규 인프라 사실상 없음 — 기존 MySQL에 `run` 테이블 1개만 추가.

---

## 1. 컴포넌트 → 인프라 매핑

| 논리 컴포넌트 | 인프라 | 신규 여부 |
|---|---|---|
| `RunController/Service/Repository` | 기존 Spring Boot 앱(단일 EC2, systemd JAR) 내부 | 코드만(인프라 무관) |
| `Run` 엔티티 → `run` 테이블 | 기존 로컬 MySQL(:3306), `ddl-auto=update` 자동 생성 | **신규 테이블 1개** |
| `(userId, finishedAt)` 인덱스 | 위 `run` 테이블 인덱스(JPA `@Index` → DDL) | 신규(테이블 부속) |
| polyline `MEDIUMTEXT` 컬럼 | MySQL MEDIUMTEXT(≈16MB) | 신규(컬럼 타입) |
| courseName lookup(`CourseRepository`) | 기존 `course` 테이블 조회 | 재사용 |
| 인증/보안 필터, `@AuthUser` | 기존 SecurityConfig/필터(U1-b/U4) | 재사용 |

## 2. 스토리지
- **DB**: 기존 MySQL(코로케이션). `run` 테이블은 부팅 시 `ddl-auto=update`로 생성.
- **컬럼**: `polyline` = `MEDIUMTEXT`(NFR Design 2.3, 절단 방지). 나머지 스칼라(Long/int/Instant/varchar).
- **Redis 미사용**: run은 캐시/세션 불필요 → Redis 손대지 않음(refresh/JWKS 캐시는 U4 전용).

## 3. 네트워킹
- **신규 egress 없음**: run 도메인은 외부 API 호출 없음(TourAPI/소셜 무관). 아웃바운드 규칙 변경 불필요.
- 인바운드: 기존 nginx(:443 TLS) → 앱(:8080). `/runs/**`는 인증 필요(deny-by-default) — 방화벽/포트 변경 없음.

## 4. 시크릿·설정
- **신규 시크릿 없음**. `.env`/EnvironmentFile 변경 없음(JWT_SECRET 등은 U4 상속).
- application.properties 신규 키 없음(run은 상속 설정만 사용).

## 5. 모니터링·공유 인프라
- 기존 구조화 로깅 상속. run 저장/조회/검증실패/소유권위반(404) 관측 지점 추가(코드 로깅 수준, 인프라 변경 없음).
- 공유 인프라 변경 없음 → `shared-infrastructure.md` 신규 작성 불필요.
