# Infrastructure Design (Light) — U1-a 공개 기반

> 확정 답변: I1=X(**AWS 단일 EC2에 앱+MySQL+Redis 코로케이션**), I2=B(플레인 JAR `java -jar`), I3=A(개발은 로컬 Redis), I4=A(env + `.env` git 제외), I5=C(로깅/모니터링 인프라 defer).
> 성격: **light**. U1-a는 영속 엔티티 없음 → 앱 자체는 MySQL 미사용이나, EC2에는 후속 유닛(U4/U5)용으로 MySQL을 함께 프로비저닝.

---

## 1. 논리 → 물리 인프라 매핑

| 논리 컴포넌트 (nfr-design) | 물리 인프라 | 비고 |
|---|---|---|
| Spring Boot 앱 | **EC2 단일 인스턴스** 위 **플레인 JAR**(`java -jar`) | systemd 서비스로 기동, 포트 8080 |
| Redis 캐시 | **동일 EC2**의 Redis 프로세스(:6379) | 개발은 로컬 Redis(I3=A) |
| MySQL | **동일 EC2**의 MySQL(:3306, `dallyeo`) | U1-a 미사용, U4/U5 대비 프로비저닝 |
| TourApiExecutor 스레드풀 | 앱 프로세스 내부(별도 인프라 없음) | JVM 스레드 |
| 외부 TourAPI | **아웃바운드 HTTPS egress**(인터넷) | apis.data.go.kr:443 |
| 시크릿(serviceKey 등) | **환경변수 + `.env`(git 제외)** | systemd `EnvironmentFile` 또는 셸 env |
| 구조화 로깅 | **stdout/파일**(현 단계) | 중앙 수집/APM은 **defer**(I5=C) |

---

## 2. 컴퓨트 (Compute)
- **AWS EC2 단일 인스턴스** — 소규모(N1=A)라 단일로 충분.
- **실행 형태(I2=B)**: Gradle 빌드 산출 **fat JAR**를 `java -jar`로 실행. **systemd 유닛**으로 관리(자동 재시작, 부팅 기동).
- JVM 옵션/힙은 인스턴스 사양에 맞춰 Build&Test 이후 튜닝(현 단계 기본값).
- 스레드 격리: `TourApiExecutor`(D6) 고정 풀 — 인스턴스 vCPU 기준으로 크기 조정.

## 3. 스토리지 (Storage)
- **Redis**(:6379) — 캐시(BR-6, TTL 30분+). U1-a에서 실사용.
  - 개발: 로컬 Redis(I3=A). 운영: 동일 EC2 Redis 프로세스.
  - EC2 재시작 시 캐시 유실 허용(캐시는 best-effort, stale 미사용 D4와 일관).
- **MySQL**(:3306, `dallyeo`) — U1-a는 미사용(영속 엔티티 없음). EC2에 설치만 해두고 U4/U5에서 스키마·연결 활성.
- 코로케이션 특성: 앱·Redis·MySQL이 같은 호스트 → localhost 접속, 네트워크 지연 최소·보안그룹 단순. 단, **단일 장애점(SPOF)** — 소규모 단계에서 수용(운영 확장 시 분리 검토, defer).

## 4. 네트워킹 (Networking)
- **인바운드**: 8080(앱). 프론트/클라이언트 접근. (HTTPS 종단·리버스프록시는 후속 결정, 현 단계 defer.)
- **아웃바운드**: TourAPI **443 HTTPS egress** 필요 — EC2 보안그룹/서브넷에서 아웃바운드 443 허용.
- Redis/MySQL은 **localhost 바인딩**(외부 노출 금지) — 코로케이션이라 외부 포트 개방 불필요(보안).
- 로드밸런서/API 게이트웨이: 단일 인스턴스라 현 단계 불필요(defer).

## 5. 시크릿·설정 (Secrets & Config) — I4=A, N3
- `TOURAPI_SERVICE_KEY` 등은 **환경변수**로 주입. systemd `EnvironmentFile=/etc/dallyeo/dallyeo.env`(권한 제한) 또는 셸 env.
- `.env`는 **git 제외**(.gitignore). 코드/VCS에 실제 값 저장 금지.
- `application.properties`는 `${TOURAPI_SERVICE_KEY}` 참조만.
- 클라우드 시크릿 매니저(Secrets Manager/SSM)는 미채택 — 필요 시 후속 승격(defer).

## 6. 모니터링 (Monitoring) — I5=C (defer)
- 현 단계: **구조화 로그를 stdout/파일**로. Resilience/캐시/미매핑 WARN 이벤트 로깅(N6).
- 중앙 로깅(CloudWatch/ELK)·APM·알람은 **차후 결정**. 로그 포맷은 후속 수집기 연동을 고려해 구조화 유지.

## 7. 미적용/보류 (N/A / Deferred)
| 항목 | 상태 | 사유 |
|---|---|---|
| 오토스케일링/멀티 AZ | N/A | 소규모 단일 인스턴스 |
| 로드밸런서/API GW | Deferred | 단일 인스턴스, 후속 |
| 매니지드 Redis/RDS | Deferred | 코로케이션 우선(I1=X, I3=A) |
| 시크릿 매니저 | Deferred | env 주입으로 충분(I4=A) |
| 중앙 로깅/APM | Deferred | I5=C |
| 메시징/큐 | N/A | 이벤트 기반 요구 없음 |

---

## 8. U1-a 인프라 산출 경계
- **U1-a 반영**: EC2 단일·JAR/systemd 실행 전제, Redis 캐시(로컬/코로케이션), env 시크릿 주입, TourAPI egress. 아웃바운드 443 요건.
- **후속 유닛**: MySQL 스키마 활성(U4/U5), HTTPS 종단·프록시, 중앙 로깅/모니터링, 매니지드 서비스 승격 검토.
