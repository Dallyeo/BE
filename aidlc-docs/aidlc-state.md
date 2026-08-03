# AI-DLC State Tracking

## Project Information
- **Project Type**: Brownfield
- **Start Date**: 2026-07-05T00:00:00Z
- **Current Stage**: CONSTRUCTION - Unit U4 (인증·사용자 🔒, U1-b 포함) - COMPLETE (Code Generation 완료, 승인 대기 → 다음 유닛 U5)

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
**U4 인증·사용자 🔒 (U1-b 포함, 진행 중)**
- [x] Functional Design (U4) - COMPLETE (approved 2026-08-03) — 결정 Q1=B(Kakao+Apple 실구현) Q2=A(토큰검증,추후 code전환 가능) Q3=A(provider+providerUserId 유니크) Q4=A(닉네임 자동생성) Q5=A(HS256) Q6=A(1세션/회전) Q7=A(하드삭제) Q8=B(onboardingCompleted 플래그). 산출물 3종(domain-entities/business-logic-model/business-rules). U1-b(SecurityConfig deny-by-default+화이트리스트/JwtAuthenticationFilter/JwtProvider/OAuthClient) 통합.
- [x] NFR Requirements (U4) - COMPLETE (approved 2026-08-03) — Q1=A(nimbus Apple JWK) Q2=A(레이트리밋 미적용) Q3=A(refresh SHA-256 해시) Q4=A(소셜호출 Resilience4j+JWKS캐시) Q5=A(at-rest accepted risk). 의존성: jjwt + nimbus-jose-jwt. 산출물 nfr-requirements/tech-stack-decisions.
- [x] NFR Design (U4, light) - COMPLETE (approved 2026-08-03) — stateless JWT + deny-by-default 화이트리스트, OAuth 전략어댑터(Kakao RestClient/Apple nimbus JWKS+캐시), refresh 회전+SHA-256 해시, oauth 엔드포인트별 Resilience4j. 산출물 nfr-design-patterns/logical-components.
- [x] Infrastructure Design (U4, light) - COMPLETE (approved 2026-08-03) — 단일 EC2 코로케이션 상속 + users 테이블(ddl-auto=update, provider+providerUserId 유니크) + Redis refresh(해시,7d)/Apple JWKS 캐시 + 아웃바운드 443(kakao/apple) + 신규 시크릿(JWT_SECRET/APPLE_CLIENT_ID). 산출물 infrastructure-design/deployment-architecture.
- [x] Code Generation (U4) - COMPLETE (승인 대기) — user/auth/external.oauth 신규 + SecurityConfig(deny-by-default)/RestClientConfig/build.gradle/application.properties/.env.example 수정. jjwt+nimbus. compile+U4 단위테스트 35건 통과. 스토리 US-AUTH-1~5·US-USER-1~3 완료.

**U4 인증·사용자 🔒 전체 완료 → 승인 시 다음 유닛 U5(러닝기록·코스생성) 또는 부팅/통합 검증**
- [ ] Unit U5 (러닝기록·코스생성 🔒) - per-unit loop 대기 (다음)
- [ ] Build and Test - 전 유닛 완료 후 EXECUTE

### 🟡 OPERATIONS PHASE
- [ ] Operations - PLACEHOLDER
