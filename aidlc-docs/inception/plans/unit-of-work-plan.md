# Unit of Work Plan — Dallyeo

시스템을 개발 단위(Unit of Work)로 분해하기 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X로 직접 설명)을 적어주세요. 모두 채운 뒤 "완료"라고 알려주시면 유닛 산출물을 생성합니다.

> **배포 형태**: 모놀리식 단일 배포(Spring Boot). 따라서 각 "유닛"은 **독립 배포 서비스가 아니라 한 앱 안의 논리 모듈 + 빌드 순서**입니다.
> 이미 실행계획에서 **공개 우선 5유닛**을 제안했고, 아래는 그 확정용 질문입니다.

---

## Part A. 분해 실행 체크리스트 (답변 확정 후 실행)
- [x] 유닛 정의·책임 (`unit-of-work.md`)
- [x] 유닛 의존 매트릭스 (`unit-of-work-dependency.md`)
- [x] 스토리 ↔ 유닛 매핑 (`unit-of-work-story-map.md`)
- [x] 코드 조직 전략 문서화 (`unit-of-work.md` 내)
- [x] 유닛 경계·의존 검증 / 모든 스토리 유닛 배정 확인

**확정 답변**: U1=A(5유닛), U2=A(순차 수직완성), U3=A(각 도메인 유닛에 데이터로더 포함), U4=A(U1 공통기반부터).
**추가 확정**: U1을 **U1-a / U1-b 2단계**로 분리 —
- **U1-a (지금)**: 공통 응답 래퍼, 전역 예외 처리, TourApiClient + 설정, SecurityConfig(전체 permitAll 껍데기), 로거 설정 교정
- **U1-b (U4 착수 시)**: JWT 필터, 토큰 발급/검증, 화이트리스트 정교화(deny-by-default), 소셜 로그인 검증, JWT 라이브러리 추가
- 근거: U2·U3이 모두 공개 API → U1-a 이후 인증 없이 공개 기능 구현 가능. 인증 구현은 실제 필요 시점(U4)에.

---

## 제안 유닛 (실행계획 기준)
| U | 유닛 | 인증 | 스토리 |
|---|---|---|---|
| 1 | 공통·기반 | — | US-COMMON-1~4, US-AUTH-4 |
| 2 | 지역·코스조회 | 🌐 | US-REGION-1, US-COURSE-1, US-COURSE-2 |
| 3 | 장소·배지 | 🌐 | US-PLACE-1~4, US-BADGE-1 |
| 4 | 인증·사용자 | 🔒 | US-AUTH-1~3·5, US-USER-1~3 |
| 5 | 러닝기록·코스생성 | 🔒 | US-RUN-1~3, US-COURSE-3 |

---

## Part B. 분해 결정 질문

### Question U1 — 유닛 경계/개수
위 5유닛 구성을 확정할까요?

A) **5유닛 그대로** (공통기반 / 지역·코스조회 / 장소·배지 / 인증·사용자 / 러닝·코스생성) (권장)
B) **더 잘게** — 예: 장소 ↔ 배지 분리, 인증 ↔ 사용자 분리 (유닛 수↑, 단위 작아짐)
C) **더 크게** — 예: 지역·코스·장소를 하나의 "조회" 유닛으로 통합
X) Other

[Answer]: A

---

### Question U2 — 유닛 진행 방식 (CONSTRUCTION 반복)
각 유닛을 어떻게 완성해 나갈까요?

A) **순차 수직완성** — 한 유닛을 설계→코드→테스트까지 끝내고 다음 유닛 (권장, 조기 동작 확인)
B) **가로 분할** — 전 유닛 설계 먼저 → 전 유닛 코드
X) Other

[Answer]: A

---

### Question U3 — 초기 데이터 적재(코스 시드 / 배지 CSV) 배치
`courses.json` 시드와 배지 CSV 적재 로직을 어디에 둘까요?

A) **각 도메인 유닛에 포함** — CourseDataLoader→U2, BadgeCsvLoader→U3 (권장)
B) **별도 부트스트랩/마이그레이션 유닛**으로 분리
X) Other

[Answer]: A

---

### Question U4 — 시작 유닛 확인
첫 구현 유닛을 확정합니다.

A) **U1 공통·기반부터** — 응답래퍼·예외·Security·JWT·TourApiClient 기반 먼저 (권장, 나머지 전 유닛의 전제)
B) 다른 유닛 먼저 (X에 지정)
X) Other

[Answer]: A

---

## Part C. 생성할 산출물 (확정 후)
- [x] `aidlc-docs/inception/application-design/unit-of-work.md`
- [x] `aidlc-docs/inception/application-design/unit-of-work-dependency.md`
- [x] `aidlc-docs/inception/application-design/unit-of-work-story-map.md`
