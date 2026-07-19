# Infrastructure Design (Light) — U2 지역·코스조회 🌐

> 확정: I1=A(`ddl-auto=update` 유지). U1-a AWS 단일 EC2 코로케이션 상속, **MySQL 활성화**만 추가.

---

## 1. 논리 → 물리 매핑 (U2 신규분)

| 논리 컴포넌트 | 물리 인프라 | 비고 |
|---|---|---|
| `course` 테이블 | **EC2 코로케이션 MySQL**(:3306, db `dallyeo`) | U1-a에서 설치만·U2에서 활성 |
| CourseRepository(JPA) | 앱 프로세스 ↔ localhost MySQL | HikariCP 기존 |
| courses.json 시드 | **classpath 리소스** `src/main/resources/data/courses.json` | 앱 JAR에 번들, 기동 시 로드 |
| Region/Course 조회 | 앱 프로세스 내부 | 외부 인프라 없음 |

- Redis는 U2에서 **미사용**(캐시 안 함, R1=A). TourAPI egress도 무관(U2는 외부 API 없음).

## 2. 스토리지 (Storage)
- **MySQL**(:3306, `dallyeo`) 활성. `course` 테이블은 **`ddl-auto=update`로 자동 생성**(I1=A).
  - 컬럼: id/name/region/declared·measuredCategory/searchOption/totalMeters/waypointCount + polyline·cumulativeMeters·waypointAnchors(TEXT).
  - 인덱스: region, measuredCategory(R2).
- localhost 바인딩(외부 미노출) 유지. 자격증명은 env(`DB_USERNAME`/`DB_PASSWORD`, 이미 정의).

## 3. 컴퓨트/네트워킹 (상속)
- 컴퓨트: EC2 단일 JAR/systemd(U1-a). 신규 없음.
- 네트워킹: 인바운드 8080. U2는 외부 아웃바운드 불필요. Redis/MySQL localhost.

## 4. 시크릿·설정 (상속)
- DB 자격증명 env 주입(U1-a `.env`/EnvironmentFile). 신규 시크릿 없음.
- courses.json은 시크릿 아님 → classpath 번들.

## 5. 배포 영향
- 앱 JAR에 `data/courses.json` 포함. 기동 시 자동 시드(멱등). 별도 인프라 작업 없음.
- 최초 기동 시 MySQL에 `course` 테이블 생성 + 10코스 삽입.

## 6. N/A / Deferred (상속)
- 매니지드 RDS 승격, Flyway 마이그레이션(I1=B) → deferred.
- 캐시/Redis, 오토스케일링/DR, 중앙 로깅 → N/A/deferred(U1-a와 동일).
