# Infrastructure Design Plan (Light) — U2 지역·코스조회 🌐

U2는 U1-a 인프라(AWS 단일 EC2 코로케이션) 위에 **MySQL 활성화**만 추가하면 됩니다. 나머지는 U1-a 상속.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **상속(U1-a)**: AWS 단일 EC2에 app(JAR/systemd)+MySQL+Redis 코로케이션, env 시크릿(DB_USERNAME/DB_PASSWORD 이미 정의), 로깅 defer.
> **U2 신규**: `course` 테이블 활성(U1-a에서 MySQL 설치만 해둠), classpath `data/courses.json` 시드.

---

## Part A. 실행 체크리스트
- [x] 설계 산출물 분석
- [x] 결정 질문 수집(Part B)
- [x] `infrastructure-design.md` — MySQL 활성/시드 매핑
- [x] `deployment-architecture.md` — U1-a 토폴로지 + course 테이블 반영

---

## Part B. 결정 질문

### Question I1 — DB 스키마 관리 정책
`course` 테이블 스키마를 어떻게 관리할까요? (현재 `spring.jpa.hibernate.ddl-auto=update`)

A) **현행 유지: `ddl-auto=update`** — 엔티티 기준 자동 생성/변경. 개발·초기엔 단순 (권장, 현 단계)
B) **Flyway/Liquibase 마이그레이션 도입** — 명시적 스키마 버전 관리(운영 안정성↑, 설정 추가)
C) 위임
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u2-region-course/infrastructure-design/infrastructure-design.md`
- [x] `aidlc-docs/construction/u2-region-course/infrastructure-design/deployment-architecture.md`
