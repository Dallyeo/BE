# Infrastructure Design Plan (Light) — U1-a 공개 기반

논리 컴포넌트를 실제 인프라로 매핑하기 위한 **계획 + 질문**입니다.
실행계획상 **경량(light)** — U1-a는 외부 TourAPI 연동 + Redis 캐시 + Spring Boot 단일 앱이 전부(영속 엔티티 없음). 그래서 최소한만 확정하고, 미정 항목은 **defer** 가능합니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **현재 사실**: 배포 모델 미정(로컬 개발만: app:8080, MySQL:3306, Redis:6379). 외부 의존은 TourAPI(HTTPS egress). U1-a는 MySQL 미사용(영속 엔티티 없음).
> **매핑 대상 논리 컴포넌트**(nfr-design/logical-components.md): Spring Boot 앱, Redis 캐시, TourApiExecutor 스레드풀, 외부 TourAPI egress, 시크릿(serviceKey), 구조화 로깅.

---

## Part A. 실행 체크리스트
- [x] 설계 산출물 분석 (functional/nfr-design)
- [x] 결정 질문 수집(Part B) — I1=X(AWS단일EC2 코로케이션), I2=B(JAR), I3=A(로컬Redis), I4=A(env), I5=C(defer)
- [x] `infrastructure-design.md` 생성 — 컴포넌트→인프라 매핑
- [x] `deployment-architecture.md` 생성 — 배포 토폴로지(경량)

---

## Part B. 결정 질문 (light — 미정이면 defer)

### Question I1 — 배포 대상/호스팅
어디에 배포할 계획인가요? (지금 확정 어려우면 defer)

A) **AWS** — EC2 또는 컨테이너(ECS/EKS), Redis는 ElastiCache
B) **단일 VM/온프레미스** — 한 서버에 앱+Redis(+MySQL) 함께
C) **아직 미정 → defer** — 로컬/개발 기준으로만 설계, 배포 선택은 Build&Test 이후로 (권장, light)
X) Other (예: GCP, Azure, Naver Cloud 등)

[Answer]: X AWS에 올릴건데 EC2 인스턴스에 MySQL, Redis 전부 같이 올릴예정

---

### Question I2 — 패키징/실행 형태
앱을 어떻게 패키징/실행할까요?

A) **Docker 컨테이너** — Dockerfile + (로컬은 docker-compose로 app+redis) (권장, 이식성·env 주입 용이)
B) **플레인 JAR** — `java -jar`, 환경변수는 셸/systemd로
C) 미정 → defer
X) Other

[Answer]: B

---

### Question I3 — Redis 프로비저닝 (캐시)
캐시용 Redis를 어떻게 둘까요?

A) **개발/현 단계: 로컬·컨테이너 Redis** — 운영은 배포 확정 시 매니지드로 승격 (권장, light)
B) **처음부터 매니지드**(ElastiCache 등) — I1이 AWS일 때
C) 미정 → defer
X) Other

[Answer]: A 개발을 진행할때는 로컬에서 Redis 사용해

---

### Question I4 — 시크릿/설정 주입 방식 (배포 환경)
`TOURAPI_SERVICE_KEY` 등 시크릿을 배포 환경에 어떻게 주입할까요? (NFR N3=env 확정, 여기선 "주입 경로")

A) **환경변수 + 로컬 `.env`(git 제외)** — 컨테이너/셸 env로 주입 (권장, 단순)
B) **클라우드 시크릿 매니저**(AWS Secrets Manager/SSM 등) — I1 클라우드 시
C) 미정 → defer (일단 env 기준)
X) Other

[Answer]: A

---

### Question I5 — 로깅/모니터링 인프라
구조화 로그(N6)를 어디로 보낼까요?

A) **stdout/파일 로깅** — 초기엔 표준출력, 배포 시 수집기(선택) (권장, light)
B) **중앙 로깅/APM**(CloudWatch/ELK/Datadog 등) 즉시 연동
C) 미정 → defer
X) Other

[Answer]: C 로깅과 모니터링은 차후에 결정하자

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u1a-foundation/infrastructure-design/infrastructure-design.md`
- [x] `aidlc-docs/construction/u1a-foundation/infrastructure-design/deployment-architecture.md`
