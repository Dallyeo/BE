# Requirements 명확화 질문 (Requirements Verification Questions)

아래 각 질문의 `[Answer]:` 뒤에 알파벳(예: A)을 적어 답변해 주세요.
옵션이 맞지 않으면 마지막 **X) Other**를 선택하고 `[Answer]:` 뒤에 직접 설명해 주세요.
모두 작성하신 후 "완료" 또는 "done"이라고 알려주시면 다음 단계로 진행합니다.

---

## 배경
현재 "Dallyeo" 프로젝트는 Spring Boot 스캐폴딩만 존재하며(비즈니스 로직 없음), 구성된 스택으로 보아 인증(JWT) 기반의 웹 REST 백엔드를 의도한 것으로 보입니다. 무엇을 만들지 정의가 필요합니다.

---

## Question 1
이번 AI-DLC 사이클에서 **무엇을 만들고 싶으신가요?** (Dallyeo 서비스의 도메인/목적)

A) 러닝/운동 기록 서비스 (프로젝트명 "달려/Dallyeo"에서 유추)
B) 소셜/커뮤니티 서비스
C) 커머스/예약 서비스
D) 아직 도메인 미정 — 인증/회원 등 공통 기반부터 구축
X) Other (please describe after [Answer]: tag below)

[Answer]: A 러닝/운동 기록 서비스는 맞는데 
그냥 단순한 러닝/운동 기록 서비스가 아니라 우선은 전라도 지역을 대상으로 기존에 짜여있는 러닝 코스를 달리거나, 러닝 코스를 직접 짜는 방식으로
러닝 이후 먹거리를 추천해주는 서비스를 진행하려고 하는거야. 그리고 그 주변에 있는 편의시설, 관광지, 음식집도 보여주는 그런 어플을 만드려고해.

---

## Question 2
이번 사이클의 **범위(scope)**는 어느 정도인가요?

A) 첫 번째 핵심 기능 하나만 (예: 회원가입/로그인)
B) 여러 관련 기능을 묶은 하나의 모듈 (예: 인증 + 회원 관리)
C) 전체 애플리케이션의 뼈대(도메인 여러 개) 한 번에
X) Other (please describe after [Answer]: tag below)

[Answer]: X 우선 작업 범위는 어떤 어플을 만들지 알고 그걸 정리해서 나누는 식으로 진행할거야.
작업 범위가 완벽해질때까지 범위를 나누지마.

---

## Question 3
가장 먼저 구현할 **핵심 기능**은 무엇인가요? (여러 개면 X에 나열)

A) 회원가입 + 로그인(JWT 발급/검증) + 인증 미들웨어
B) 위 인증 + 사용자 프로필 CRUD
C) 도메인 핵심 기능부터 (Question 1의 도메인 관련)
X) Other (please describe after [Answer]: tag below)

[Answer]: X 이것도 질문2와 마찬가지

---

## Question 4
**인증 방식**을 확정하겠습니다. 설정 파일에 JWT(액세스 1시간 / 리프레시 7일)가 이미 구성되어 있습니다.

A) JWT 기반 (액세스 + 리프레시 토큰, 리프레시 토큰은 Redis 저장) — 기존 설정 유지
B) JWT 액세스 토큰만 사용 (리프레시 미사용)
C) 세션/쿠키 기반으로 변경
X) Other (please describe after [Answer]: tag below)

[Answer]: A로 할거야. 다만 액세스 시간을 조금 길게 가져갈 생각을 하고 있어.

---

## Question 5
리버스 엔지니어링에서 발견된 **설정 이슈**를 이번 사이클에서 함께 정리할까요?
(① `logging.level.com.celdog` → `com.ppip`로 수정, ② JWT 라이브러리 의존성 추가, ③ 스키마 마이그레이션 도구(Flyway/Liquibase) 도입 여부)

A) ①②만 정리 (로거 수정 + JWT 라이브러리 추가), 마이그레이션 도구는 나중에
B) ①②③ 모두 정리 (마이그레이션 도구까지 도입)
C) 지금은 손대지 않고 기능 구현에만 집중
X) Other (please describe after [Answer]: tag below)

[Answer]: A 우선 로거 수정이랑 라이브러리 추가는 진행해. 마이그레이션 도구는 내가 뭔지 몰라서 아직은 도입 하지마. 그리고 마이그레이션 도구에 대해서 설명을 해줘

---

## Question 6
**API 응답/에러 형식**에 대한 선호가 있나요?

A) 공통 응답 래퍼 사용 (예: `{ success, data, error }` 형태 + 전역 예외 처리)
B) 표준 HTTP 상태 코드 + 순수 DTO (래퍼 없음)
C) 아직 결정 안 됨 — 추천해 주세요
X) Other (please describe after [Answer]: tag below)

[Answer]: A 공통 응답 래퍼를 사용할건데 아직 구조는 완벽히 나오지는 않은 상태야. 우선 공통 응답 래퍼를 사용할거다 라고만 알고있으면 돼

---

## Question: Security Extensions
Should security extension rules be enforced for this project?
(보안 확장 규칙을 이 프로젝트에 강제 적용할까요? — 인증/토큰을 다루므로 권장)

A) Yes — 모든 SECURITY 규칙을 blocking 제약으로 강제 (프로덕션급 애플리케이션 권장)
B) No — SECURITY 규칙 생략 (PoC/프로토타입/실험용에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: X 보안 규칙은 정하되 보안이 불필요한 API 요청이 있을 수 있어. 그거는 적용을 안하겠지

---

## Question: Property-Based Testing Extension
Should property-based testing (PBT) rules be enforced for this project?
(속성 기반 테스트(PBT) 규칙을 강제 적용할까요?)

A) Yes — 모든 PBT 규칙을 blocking 제약으로 강제 (비즈니스 로직/직렬화/상태 컴포넌트가 있는 프로젝트 권장)
B) Partial — 순수 함수 및 직렬화 왕복(round-trip)에 대해서만 PBT 강제
C) No — 모든 PBT 규칙 생략 (단순 CRUD/얇은 통합 계층에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: C 우선은 외부 API를 쓰는 빈도가 높을 가능성이 있고, 단순 CRUD를 하는것들이 많을거야. 그래서 우선은 생략하고, 나중에 말하면 추가하는거로 진행하자
