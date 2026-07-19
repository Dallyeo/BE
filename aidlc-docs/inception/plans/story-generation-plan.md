# Story Generation Plan — Dallyeo

이 문서는 요구사항을 사용자 스토리로 변환하기 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳을 적어주시고, 맞는 게 없으면 **X) Other**로 직접 설명해 주세요.
모두 작성하신 후 "완료"라고 알려주시면 스토리를 생성합니다.

---

## Part A. 스토리 생성 방법론 (실행 체크리스트)

아래는 사용자 답변 확정 후 실행할 단계입니다 (지금은 참고용):

- [x] 페르소나 정의 (`personas.md`)
- [x] 요구사항(FR-1~FR-6)을 스토리로 매핑
- [x] INVEST 기준으로 스토리 작성 (Independent, Negotiable, Valuable, Estimable, Small, Testable)
- [x] 각 스토리에 인수 조건(acceptance criteria) 작성
- [x] 페르소나 ↔ 스토리 매핑
- [x] 스토리를 에픽/그룹으로 조직화

---

## Part B. 명확화 질문

### Question S1 — 스토리 조직화(그룹핑) 방식
스토리를 어떤 기준으로 묶을까요?

A) **도메인 기반(Domain-Based)** — 인증 / 코스 / 러닝기록 / 추천 등 도메인별 그룹 (범위 분할에 유리)
B) **사용자 여정 기반(User Journey-Based)** — 앱 사용 흐름(로그인→코스 탐색→러닝→추천) 순서로 구성
C) **에픽 기반(Epic-Based)** — 큰 에픽 아래 세부 스토리 계층 구조
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question S2 — 페르소나 범위
어떤 사용자 유형을 페르소나로 정의할까요? (여러 개면 X에 나열)

A) **러너(일반 사용자) 1종만** — 관리자는 이번 범위 밖이므로 제외
B) **러너 + 운영자(데이터 적재 주체)** — 운영자는 앱이 아닌 직접 적재 담당으로만 포함
C) 러너를 세분화 (예: 코스를 직접 만드는 러너 vs 기존 코스만 달리는 러너)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question S3 — 스토리 세분화(Granularity) 수준
스토리를 얼마나 잘게 쪼갤까요?

A) **중간 수준** — 기능 단위 (예: "소셜 로그인", "코스 목록 조회", "러닝 기록 저장")
B) **세밀한 수준** — API/화면 동작 단위로 더 잘게 (예: 로그인을 카카오/애플로 분리)
C) **큰 수준** — 에픽에 가깝게 크게 (예: "인증", "코스 관리")
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question S4 — 인수 조건(Acceptance Criteria) 형식
각 스토리의 인수 조건을 어떤 형식으로 쓸까요?

A) **Given-When-Then** (BDD 스타일, 테스트 작성에 유리)
B) **체크리스트형** (충족 조건을 불릿으로 나열, 간결)
C) 추천해 주세요
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question S5 — 비기능/기술 스토리 포함 여부
인증·공통 응답 래퍼·외부 API 연동 같은 기술적 항목도 스토리로 넣을까요?

A) **포함** — 기능 스토리 + 기술/기반 스토리(인증, 응답 래퍼, 연동) 모두 작성
B) **기능 위주** — 사용자 대면 기능만 스토리로, 기술 항목은 요구사항 문서로만 유지
X) Other (please describe after [Answer]: tag below)

[Answer]: A

---

### Question S6 — 우선순위 표기
스토리에 우선순위를 표기할까요?

A) **표기함** — MoSCoW(Must/Should/Could/Won't) 또는 High/Medium/Low
B) **표기 안 함** — 우선순위는 Workflow Planning 단계에서 다룸
X) Other (please describe after [Answer]: tag below)

[Answer]:A

---

## Part C. 필수 산출물 (확정 후 생성)
- [x] `aidlc-docs/inception/user-stories/stories.md`
- [x] `aidlc-docs/inception/user-stories/personas.md`
