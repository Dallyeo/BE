# Deployment Architecture (Light) — U3 장소·배지 🌐

> U1-a/U2 토폴로지 상속 + U3: badge 테이블, CSV 리소스, TourAPI egress·Redis 캐시 실사용.

---

## 1. 토폴로지

```
              [ Client / Frontend ]
                     │ HTTP :8080
                     ▼
  ┌───────────────────────────────────────────────┐
  │ AWS EC2 (single, 코로케이션)                    │
  │  Spring Boot 앱 (java -jar, systemd)            │
  │   - PlaceController / PlaceService              │
  │   - PlaceDetailAssembler (common→intro)         │
  │   - BadgeCsvLoader ─ 기동 시 CSV 적재            │
  │        │ classpath: data/*.csv (UTF-8/EUC-KR)   │
  │        ▼ localhost:3306                         │
  │   MySQL: badge(+course) 테이블                   │
  │   Redis:6379 ── TourAPI 응답 캐시(실사용)         │
  └───────────────┬───────────────────────────────┘
                  │ outbound HTTPS :443 (실호출)
                  ▼
        [ TourAPI  apis.data.go.kr ]
```

## 2. 기동 시퀀스 (U3)
```
앱 부팅
 → JPA: badge 테이블 자동 생성(ddl-auto=update)
 → BadgeCsvLoader(ApplicationRunner):
      모범음식점 CSV(UTF-8) → MODEL_RESTAURANT (군산, 정규화 저장)
      착한가격 CSV(EUC-KR)  → 요식업만 GOOD_PRICE
      (type,normName,normAddr) 멱등 스킵
 → 서빙: /places/search|nearby|{id} 등 (TourAPI 실호출 + Redis 캐시)
```

## 3. 배포 절차 (추가 작업 없음)
- fat JAR(CSV·설정 포함) → EC2 → systemd restart. 신규 인프라 없음.
- **필수 확인**: `TOURAPI_SERVICE_KEY` env 존재(실호출), 아웃바운드 443 허용.

## 4. 리스크 / 후속
- **TourAPI 실시간 의존**: 장애 시 Resilience4j 폴백(502). 캐시로 완화.
- CSV 인코딩 오지정 시 배지 매칭 전멸 → 파일별 Charset 필수(NFR).
- ddl-auto=update, SPOF, HTTPS 종단 → U1-a/U2와 동일(deferred).
