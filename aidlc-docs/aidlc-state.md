# AI-DLC State Tracking

## Project Information
- **Project Type**: Brownfield
- **Start Date**: 2026-07-05T00:00:00Z
- **Current Stage**: CONSTRUCTION - Unit U3 (장소·배지 🌐) - COMPLETE (승인 대기 → 다음 유닛 U4)

## Workspace State
- **Existing Code**: Yes
- **Programming Languages**: Java 17
- **Build System**: Gradle (Spring Boot 4.0.6)
- **Project Structure**: Monolith (Spring Boot application - "Dallyeo")
- **Reverse Engineering Needed**: Yes
- **Workspace Root**: /Users/seunghwan/dev/code/java/BE

## Code Location Rules
- **Application Code**: Workspace root (NEVER in aidlc-docs/)
- **Documentation**: aidlc-docs/ only
- **Structure patterns**: See code-generation.md Critical Rules

## Extension Configuration
| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis |
| Property-Based Testing | No | Requirements Analysis |

**Notes**:
- Security Baseline enabled. Per user (Q-Security answer X): security rules apply, but some endpoints are intentionally public (no auth). This is consistent with SECURITY-08 "deny by default, explicit public exceptions". Public endpoints must be explicitly listed and justified.
- Property-Based Testing skipped for now (mostly CRUD + external API integration). May be added later on request.

## Reverse Engineering Status
- [x] Reverse Engineering - Completed on 2026-07-05T00:00:00Z
- **Artifacts Location**: aidlc-docs/inception/reverse-engineering/

## Execution Plan Summary
- **Plan**: aidlc-docs/inception/plans/execution-plan.md
- **Stages to Execute**: Application Design, Units Planning, Units Generation, Functional Design, NFR Requirements, NFR Design, Infrastructure Design(light), Code Generation, Build & Test
- **Stages to Skip**: 없음
- **Unit 분해(확정, 공개 우선)**: U1-a 공개기반 → U2 지역·코스조회🌐 → U3 장소·배지🌐 → U4 인증·사용자🔒(U1-b 포함) → U5 러닝기록·코스생성🔒
- **U1 분리**: U1-a(응답래퍼/예외/TourApiClient/permitAll 껍데기, 지금) · U1-b(JWT/필터/화이트리스트 정교화/소셜검증, U4 착수 시)
- **Risk**: Medium (외부 TourAPI 실시간 의존)

## Stage Progress

### 🔵 INCEPTION PHASE
- [x] INCEPTION - Workspace Detection (Complete)
- [x] INCEPTION - Reverse Engineering (Complete - approved)
- [x] INCEPTION - Requirements Analysis (Complete - approved)
- [x] INCEPTION - User Stories (Complete - approved 2026-07-18; 8 도메인 21 스토리 + 보류 백로그)
- [x] INCEPTION - Workflow Planning (Complete - execution-plan.md 작성; 승인 대기)
- [x] INCEPTION - Application Design - COMPLETE (설계 산출물 5종; 승인 대기)
- [x] INCEPTION - Units Planning - COMPLETE (5유닛, U1 분리 U1-a/U1-b)
- [x] INCEPTION - Units Generation - COMPLETE (unit-of-work 3종; 승인 대기)

### 🟢 CONSTRUCTION PHASE (per-unit)
**U1-a 공개 기반 (진행 중)**
- [x] Functional Design - COMPLETE (U1-a, approved)
- [x] NFR Requirements - COMPLETE (U1-a, approved 2026-07-18) — nfr-requirements/tech-stack-decisions
- [x] NFR Design (U1-a) - COMPLETE (approved 2026-07-18) — nfr-design-patterns/logical-components; D2=B 엔드포인트별 서킷
- [x] Infrastructure Design (light) (U1-a) - COMPLETE (승인 대기) — AWS 단일 EC2 코로케이션(app+MySQL+Redis), JAR/systemd, env 시크릿, 로깅 defer
- [x] Code Generation (U1-a) - COMPLETE (승인 대기) — 19 steps, 25 java files + build/config/.env.example; compileJava/compileTestJava 성공, 순수 단위테스트 통과. Jackson3/Resilience4j 2.2.0 반영.

**U1-a 공개 기반 전체 완료 → 승인 시 다음 유닛 U2(지역·코스조회) 또는 Build & Test**

### 다음 유닛/단계
**U2 지역·코스조회 🌐 (진행 중)**
- [x] Functional Design (U2) - COMPLETE (승인 대기) — Course JSON컬럼 엔티티, measured 기준, 멱등 시드, RegionCatalog 표시명 확장
- [x] NFR Requirements (U2, light) - COMPLETE (승인 대기) — 캐시 미적용, region/measured 인덱스, JSON TEXT 컬럼, U1-a 상속
- [x] NFR Design (U2, light) - COMPLETE (승인 대기) — @Convert 컨버터 3종, waypointCount 컬럼, 인덱스
- [x] Infrastructure Design (U2, light) - COMPLETE (승인 대기) — MySQL 활성/ddl-auto=update, classpath 시드
- [x] Code Generation (U2) - COMPLETE (승인 대기) — Region/Course 레이어 17 java 파일 + 시드 리소스; compile+단위테스트 통과

**U2 지역·코스조회 🌐 전체 완료 → 승인 시 다음 유닛 U3**
**U3 장소·배지 🌐 (진행 중)**
- [x] Functional Design (U3) - COMPLETE (승인 대기) — 4엔드포인트, common→intro 순차, 배지 정규화 exact(업소명+주소 둘 다), 군산·요식업만, 캐싱
- [x] NFR Requirements (U3) - COMPLETE (승인 대기) — 엔드포인트별 서킷+캐시, 배지 복합인덱스, CSV 파일별 인코딩(UTF-8/EUC-KR)
- [x] NFR Design (U3) - COMPLETE (승인 대기) — 보수적 정규화, Assembler가 배지 부착, 엔드포인트별 서킷 확장
- [x] Infrastructure Design (U3, light) - COMPLETE (승인 대기) — badge 테이블 활성, CSV classpath, TourAPI egress 실사용
- [x] Code Generation (U3) - COMPLETE (승인 대기) — place/badge 20+ java, TourApiClient 4오퍼레이션 확장, CSV 2종; compile+단위테스트 통과(배지 97건 적재 검증)

**U3 장소·배지 🌐 전체 완료 → 승인 시 다음 유닛 U4(인증·사용자, U1-b 포함)**
- [ ] Unit U4 (인증·사용자 🔒, U1-b 포함) - per-unit loop 대기 (다음)
- [ ] Unit U5 (러닝기록·코스생성 🔒) - 대기
- [ ] Build and Test - 전 유닛 완료 후 EXECUTE

### 🟡 OPERATIONS PHASE
- [ ] Operations - PLACEHOLDER
