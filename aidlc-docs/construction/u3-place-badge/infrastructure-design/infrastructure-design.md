# Infrastructure Design (Light) — U3 장소·배지 🌐

> U1-a AWS 단일 EC2 코로케이션 + U2 MySQL(ddl-auto=update) 상속. U3는 badge 테이블 + CSV + TourAPI egress 실사용 추가.

---

## 1. 논리 → 물리 매핑 (U3 신규분)

| 논리 컴포넌트 | 물리 인프라 | 비고 |
|---|---|---|
| `badge` 테이블 | EC2 코로케이션 MySQL(:3306) | ddl-auto=update 자동 생성, 복합 인덱스 |
| 배지 CSV 2종 | classpath `src/main/resources/data/` | 모범음식점(UTF-8)/착한가격(EUC-KR), JAR 번들 |
| TourApiClient(장소) | **아웃바운드 HTTPS 443** → apis.data.go.kr | U1-a에서 요건만·U3에서 실호출 |
| TourAPI 응답 캐시 | **EC2 코로케이션 Redis(:6379)** | U3에서 실사용(U2는 미사용) |

## 2. 스토리지
- **MySQL**: `badge` 테이블 활성(ddl-auto=update). 인덱스 (normalizedName, normalizedAddress). course 테이블(U2) 병존.
- **Redis**: TourAPI 조회 응답 캐시(TTL 30분+) 실사용. localhost 바인딩.

## 3. 네트워킹
- **아웃바운드 443**(TourAPI) 실제 사용 — EC2 보안그룹 아웃바운드 443 허용 필요(U1-a 인프라 설계 명시).
- 인바운드 8080. Redis/MySQL localhost.

## 4. 시크릿·설정
- **`TOURAPI_SERVICE_KEY`** env 실사용(U1-a). 로그 마스킹(U1-a LogMaskingUtil).
- Resilience4j 인스턴스 4종 application.properties 추가(NFR).

## 5. 배포 영향
- 앱 JAR에 CSV 2종 + Resilience 설정 포함. 기동 시 badge 테이블 생성 + CSV 멱등 적재.

## 6. N/A / Deferred (상속)
- 매니지드 RDS/ElastiCache 승격, Flyway, 중앙 로깅 → deferred(U1-a/U2와 동일).
