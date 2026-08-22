# Deployment Architecture (Light) — U5 러닝기록 🔒

> U1-a/U2/U3/U4 토폴로지 전면 상속 + U5: `run` 테이블만 추가. 신규 인프라/시크릿/egress 없음.

---

## 1. 토폴로지 (U4 대비 변화점만)

```
              [ Client / Frontend ]
                     │ HTTPS :443
                     ▼
              [ nginx (TLS 종단) ]
                     │ http :8080
                     ▼
  ┌───────────────────────────────────────────────┐
  │ AWS EC2 (single, 코로케이션)                    │
  │  Spring Boot 앱 (java -jar, systemd)            │
  │   - 기존: Security/JWT/Auth/User/Course/Place    │
  │   - 신규: RunController/RunService/RunRepository  │  ← U5
  │        │ localhost:3306                         │
  │        ▼                                        │
  │   MySQL: users, course, badge, region ...       │
  │          + run (신규, MEDIUMTEXT polyline)  ← U5 │
  │   Redis:6379 ── (U4 refresh/JWKS만, U5 미사용)   │
  └───────────────┬───────────────────────────────┘
                  │ outbound HTTPS :443
                  └──▶ kakao / apple / TourAPI (U3/U4 상속, U5 무관)
```

변화점: **`run` 테이블 추가**뿐. 외부 egress·Redis·포트·시크릿 변화 없음.

## 2. 기동 시퀀스 (U5)
```
앱 부팅
 → env 검증: 기존과 동일(JWT_SECRET 등, 신규 없음)
 → JPA: run 테이블 자동 생성(ddl-auto=update), idx_run_user_finished 인덱스
 → SecurityConfig: 기존 deny-by-default 유지 → /runs/** 는 화이트리스트 미포함 → 자동 authenticated
 → 서빙(추가):
     🔒 POST /runs, GET /runs, GET /runs/{id} — 토큰 필요
```

## 3. 배포 절차 (경량)
- fat JAR 재빌드(신규 의존성 없음) → EC2 → systemd restart.
- **신규 인프라 없음**. 확인 항목:
  - 부팅 후 `run` 테이블·인덱스 생성 확인.
  - `/runs/**`가 미인증 시 401 반환(deny-by-default 정상).
  - MySQL `polyline` 컬럼이 `MEDIUMTEXT`로 생성됐는지 확인(장거리 러닝 절단 방지).

## 4. 리스크 / 후속
- **polyline 대용량**: 상한 미설정(NQ1). MEDIUMTEXT로 절단은 방지하나, 초대량 저장 시 DB 용량·직렬화 비용 증가 → 규모 확대 시 점 개수 상한/다운샘플링 백로그.
- **목록 무페이징**(NQ2): 기록 누적 시 응답 커질 수 있음 → 페이징 백로그.
- **계정 삭제 cascade**: U4 사용자 하드삭제 시 `run` 잔존(고아 레코드) 가능 — 삭제 정책은 U5 범위 밖(러닝 삭제 defer). Build&Test/후속에서 cascade 또는 정리 배치 검토.
- ddl-auto=update / SPOF / at-rest 미암호화 → 기존 유닛과 동일(deferred, accepted risk). 단 polyline=위치정보이므로 RDS 이관 시 암호화 우선.
