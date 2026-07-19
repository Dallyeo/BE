# NFR Requirements Plan (Light) — U2 지역·코스조회 🌐

U2의 비기능 요구를 확정하기 위한 **계획 + 질문**입니다. U2는 **조회 + JPA 영속화** 중심이라 대부분 U1-a NFR을 상속합니다. 신규 관심사(정적 코스 캐싱, DB 인덱스)만 경량으로 묻습니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **상속(U1-a)**: 소규모 단일 인스턴스, 성능 하드타깃 미설정, Security Baseline(공개 API=permitAll·입력검증), 구조화 로깅, MySQL/Redis/JPA 기존 스택.
> **U2 신규**: `course` 테이블(JSON 컬럼), 필터 조회(region/measuredCategory), 정적 10코스 시드.

---

## Part A. 실행 체크리스트
- [x] FD 분석 (조회/영속화 특성)
- [x] 결정 질문 수집(Part B)
- [x] `nfr-requirements.md` — 성능/가용성/보안/영속성
- [x] `tech-stack-decisions.md` — JPA/JSON 매핑/캐싱 결정

---

## Part B. 결정 질문

### Question R1 — 코스 데이터 캐싱
정적 10코스(읽기전용)의 목록/상세 조회를 캐싱할까요?

A) **캐시 안 함** — 10행·읽기전용이라 MySQL 직접 조회로 충분. U5(사용자 코스 생성) 시 캐시 무효화 복잡성 회피 (권장, 단순)
B) **Redis 캐시** — U1-a 캐시 인프라 재사용(목록/상세 캐시). U5 생성 시 해당 지역 캐시 무효화 필요
C) 위임
X) Other

[Answer]: A

---

### Question R2 — DB 인덱스 (필터 조회)
`region`, `measuredCategory` 필터용 인덱스를 둘까요?

A) **복합/단일 인덱스 추가** — `region`, `measuredCategory`에 인덱스(현재 10행엔 무의미하나 U5 성장 대비) (권장)
B) **인덱스 없음** — 소량이라 불필요, 필요 시 후속 추가
C) 위임
X) Other

[Answer]: A

---

### Question R3 — U1-a 정책 상속 확인
성능/보안/가용성 정책을 U1-a 그대로 상속할까요?

A) **상속** — 성능 하드타깃 미설정, 공개 API permitAll, 컨트롤러 입력검증(Bean Validation), 구조화 로깅 (권장)
B) U2 전용 목표 별도 설정
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u2-region-course/nfr-requirements/nfr-requirements.md`
- [x] `aidlc-docs/construction/u2-region-course/nfr-requirements/tech-stack-decisions.md`
