# AI-DLC State Tracking

## Project Information
- **Project Type**: Brownfield
- **Start Date**: 2026-07-05T00:00:00Z
- **Current Stage**: CONSTRUCTION - U6(업적) 추가 완료. 전 유닛(U1-a~U6) 완료. 전체 테스트 125/125 통과, 업적 e2e 라이브 검증(자동 달성/수동 unlock 409·404/목록). bootJar 패키징 성공. (별도: 코스 description 기능 U2 확장 완료.)

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
**U5 러닝기록 🔒 (진행 중)**
> **범위 변경(2026-08-22)**: US-COURSE-3(사용자 코스 생성) 백엔드 제거 — 사용자 코스는 DB 저장 안 함(프론트 책임). U5 = run 도메인만. Course(U2) 변경 없음.
- [x] Functional Design (U5) - COMPLETE (approved 2026-08-22) — 결정 Q1=Long IDENTITY(Run), Q6=완주율 계산안함(원천만 저장), Q7=courseId 느슨저장(FK검증X, 시드 참조용), Q8=finishedAt 최신순, Q9=averagePace 클라값, Q10=표준검증, Q11=수정/삭제 범위밖. (Q2~Q5 코스생성 관련은 범위 제거로 무효). 산출물 3종(domain-entities/business-logic-model/business-rules). 스토리 US-RUN-1/2/3.
- [x] NFR Requirements (U5, light) - COMPLETE (approved 2026-08-22) — NQ1=polyline 상한없음/defer(단 컬럼 MEDIUMTEXT 절단방지), NQ2=목록 페이징없음/defer(polyline 제외 경량), NQ3=신규 스택없음(전부 U1-a/U4 상속). (userId,finishedAt) 인덱스. Security Baseline SECURITY-05/08/03/15/01/12 매핑, 소유권 404 은폐, polyline 위치정보 미로깅. 산출물 nfr-requirements/tech-stack-decisions.
- [x] NFR Design (U5, light) - COMPLETE (approved 2026-08-22) — 신규 인프라 패턴 없음(외부의존 없어 서킷 미적용). 표준 JPA 3계층. 신규 7파일(Run/RunRepository/RunService/RunController/RunCreateRequest/RunSummaryResponse/RunDetailResponse), 재사용(Course/AuthUser/Converter/ExceptionHandler). 소유권 404 은폐, 경량 목록(polyline 제외), MEDIUMTEXT, (userId,finishedAt) 인덱스, courseName best-effort. 노트: 계정삭제 시 runs cascade 정책 후속 확인. 산출물 nfr-design-patterns/logical-components.
- [x] Infrastructure Design (U5, light) - COMPLETE (승인 대기) — 단일 EC2 코로케이션 전면 상속 + run 테이블 1개만 추가(ddl-auto=update, idx_run_user_finished, polyline MEDIUMTEXT). Redis 미사용, 신규 egress/시크릿/포트/의존성 없음. 산출물 infrastructure-design/deployment-architecture. 리스크: polyline 대용량/무페이징/계정삭제 cascade 고아레코드 후속검토.
- [x] Code Generation (U5) - COMPLETE (승인 대기) — 신규 7 java(run/: Run·RunRepository·RunService·RunController + dto 3), 테스트 11건(RunServiceTest 8 + RunControllerTest 3) 전부 통과, compileJava/compileTestJava 성공. 문서 수정 API.md(§7 Runs)/Postman(Runs 폴더). 변경없음: SecurityConfig/build.gradle/properties/Course/User. polyline MEDIUMTEXT, (userId,finishedAt) 인덱스, 소유권 404, completionRate null. 스토리 US-RUN-1/2/3 완료. code-summary.md.

**U5 러닝기록 🔒 전체 완료 → 승인 시 Build & Test(전 유닛)**
- [ ] NFR Requirements (U5) - per-unit loop 대기 (다음)
**U6 업적 (Achievement) 🔒 — 사용자 요청으로 추가(2026-08-22, Build&Test 이후)**
- [x] U6 COMPLETE — 신규 도메인. 결정 Q1=둘 다(자동+수동)/Q2=courseId 기반/Q3=전체+달성여부. AchievementType enum 8종(img_1.png), UserAchievement 엔티티(user_achievement 테이블), AchievementService(evaluateAndUnlock/unlock/list), AchievementController(/achievements GET, POST /{code}/unlock). RunService.save에 자동 판정 훅, RunRepository.findDistinctCourseIds 추가. 테스트 10건(Service 8 + Controller 2). 라이브 e2e 검증 통과(자동 달성 GUNSAN_BEGINNER+JJAMPPONG, 수동 미충족 409, 없는코드 404, 무토큰 401). 산출물 u6-achievement/functional-design(business-rules), code(code-summary). API.md §8 + Postman 업적 폴더.

- [x] Build and Test - COMPLETE (U6 포함 재실행: 전체 125/125 통과) (승인 대기) — 전체 테스트 115/115 통과(31 클래스, 0 실패). @SpringBootTest 컨텍스트 로드=부팅 검증(Jackson3+jjwt 공존, run 테이블/course.description DDL). 라이브 스모크: /runs 무토큰 401(deny-by-default), /courses description 노출. bootJar 78MB 패키징 성공. 지시 문서 6종(build/unit/integration/performance/security/summary). 성능=N/A(SLA 미설정 defer).

### 🟡 OPERATIONS PHASE
- [ ] Operations - PLACEHOLDER
