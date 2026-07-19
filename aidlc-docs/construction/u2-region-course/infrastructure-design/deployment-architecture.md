# Deployment Architecture (Light) — U2 지역·코스조회 🌐

> U1-a 토폴로지(AWS 단일 EC2 코로케이션) 그대로. U2는 **MySQL `course` 테이블 활성 + classpath 시드**만 반영.

---

## 1. 토폴로지 (U1-a 상속 + U2 반영)

```
              [ Client / Frontend ]
                     │ HTTP :8080
                     ▼
  ┌───────────────────────────────────────────────┐
  │ AWS EC2 (single, 코로케이션)                    │
  │                                                 │
  │  Spring Boot 앱 (java -jar, systemd)            │
  │   - RegionController / CourseController (공개)   │
  │   - CourseDataLoader ─ 기동 시 시드              │
  │        │ classpath: data/courses.json (JAR 번들)│
  │        │                                        │
  │        ▼ localhost:3306                         │
  │   ┌──────────────┐                              │
  │   │ MySQL:3306    │  db: dallyeo                 │
  │   │  course 테이블 │  (ddl-auto=update 자동생성)   │
  │   └──────────────┘                              │
  │   Redis:6379 — U2 미사용                         │
  └───────────────────────────────────────────────┘
   (U2는 외부 아웃바운드 없음 — TourAPI egress 무관)
```

## 2. 기동 시퀀스 (U2)
```
앱 부팅
 → JPA: course 테이블 자동 생성/검증(ddl-auto=update)
 → CourseDataLoader(ApplicationRunner):
      classpath data/courses.json 로드
      → 각 코스 existsById? 스킵 : (변환+waypointCount 계산+저장)
 → 준비 완료 (GET /regions, /courses, /courses/{id} 서빙)
```

## 3. 배포 절차 (U1-a와 동일, 추가 작업 없음)
- Gradle fat JAR(데이터 리소스 포함) → EC2 → systemd restart.
- 신규 인프라/시크릿 없음. MySQL은 U1-a에서 이미 설치·자격증명 env 존재.

## 4. 리스크 / 후속
- **ddl-auto=update**: 운영 스키마 변경 추적 부재 → 안정화 시 Flyway 전환(deferred).
- SPOF/HTTPS 종단: U1-a와 동일(deferred).
- 시드 갱신: 최초 1회만(Q4=B) → courses.json 내용 변경 시 반영하려면 수동 삭제/재적재 또는 향후 Upsert 옵션.
