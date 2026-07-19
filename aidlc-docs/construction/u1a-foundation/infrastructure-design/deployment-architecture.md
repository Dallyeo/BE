# Deployment Architecture (Light) — U1-a 공개 기반

> AWS 단일 EC2 코로케이션(I1=X), 플레인 JAR + systemd(I2=B), 개발은 로컬 Redis(I3=A), env 시크릿(I4=A), 로깅/모니터링 defer(I5=C).

---

## 1. 운영 토폴로지 (AWS 단일 EC2 코로케이션)

```
                    [ Client / Frontend ]
                            │  HTTP :8080 (inbound)
                            ▼
   ┌─────────────────────────────────────────────────────┐
   │  AWS EC2 (single instance)                            │
   │  Security Group: in 8080 / out 443                    │
   │                                                       │
   │   ┌───────────────────────────┐                      │
   │   │ Spring Boot 앱 (java -jar) │  systemd service     │
   │   │  :8080                     │                      │
   │   │  - TourApiClient           │──┐ localhost:6379    │
   │   │  - @Cacheable / Resilience │  │                   │
   │   │  - TourApiExecutor 풀       │  ▼                   │
   │   │  env: TOURAPI_SERVICE_KEY  │ ┌──────────────┐     │
   │   └───────────┬───────────────┘ │ Redis :6379   │     │
   │               │                 │ (캐시)         │     │
   │               │ localhost:3306  └──────────────┘     │
   │               ▼                                       │
   │        ┌──────────────┐                               │
   │        │ MySQL :3306   │  (U1-a 미사용, U4/U5 대비)     │
   │        │ db: dallyeo   │                               │
   │        └──────────────┘                               │
   └───────────────┬───────────────────────────────────────┘
                   │ outbound HTTPS :443
                   ▼
        [ 외부 TourAPI  apis.data.go.kr ]
```

- 앱·Redis·MySQL이 **동일 EC2**에 코로케이션 → 상호 접속은 **localhost**.
- Redis/MySQL은 **localhost 바인딩**(외부 미노출). 인바운드는 8080만, 아웃바운드는 TourAPI 443.

---

## 2. 개발(로컬) vs 운영(EC2) 매핑

| 항목 | 개발(로컬) | 운영(EC2) |
|---|---|---|
| 앱 실행 | IDE / `./gradlew bootRun` / `java -jar` | `java -jar` + **systemd** |
| Redis | **로컬 Redis :6379**(I3=A) | 동일 EC2 Redis :6379 |
| MySQL | 로컬 :3306(U4/U5부터) | 동일 EC2 MySQL :3306 |
| 시크릿 | `.env`(git 제외) | `EnvironmentFile`(systemd) / 셸 env |
| TourAPI | 실제 HTTPS 또는 WireMock 목킹 | 실제 HTTPS egress |
| 로깅 | 콘솔(구조화) | stdout/파일 (수집기 defer) |

---

## 3. 배포 플로우 (경량)

```
Gradle 빌드 → fat JAR (build/libs/*.jar)
   → EC2로 전송(scp/CI 등, 방식 defer)
   → /etc/dallyeo/dallyeo.env 에 시크릿 배치(권한 제한)
   → systemd: dallyeo.service (java -jar, EnvironmentFile)
   → systemctl restart dallyeo
```

- CI/CD 파이프라인 상세(빌드 자동화·아티팩트 전송)는 **defer** — Build&Test 이후 결정.
- 무중단 배포/헬스체크: 단일 인스턴스라 현 단계 단순 재시작(짧은 다운타임 수용).

---

## 4. 리스크 / 후속 과제
- **SPOF**: 단일 EC2에 전 계층 → 인스턴스 장애 = 전체 중단. 소규모 단계 수용, 확장 시 계층 분리(매니지드 Redis/RDS, 다중 AZ) 검토 — **defer**.
- **HTTPS 종단**: 8080 평문 노출 → 운영 전 리버스프록시(Nginx)/ALB로 TLS 종단 권장 — **defer**.
- **캐시 유실**: EC2/Redis 재시작 시 캐시 비워짐 → stale 미사용 정책(D4)과 일관, 재구성은 TourAPI 재호출로 자연 복구.
- **모니터링 공백**: I5=C로 중앙 로깅/알람 미구성 → 장애 감지는 수동. 후속 우선순위.
