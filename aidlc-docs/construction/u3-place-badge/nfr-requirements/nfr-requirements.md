# NFR Requirements — U3 장소·배지 🌐

> 확정: N1=A(엔드포인트별 서킷+캐시), N2=A(성능 하드타깃 미설정), N3=A(복합 인덱스+멱등 적재). U1-a/U2 상속.

---

## 1. 확장성
- 소규모 단일 인스턴스 상속. TourAPI 조회는 stateless. 배지 소량(≤112건, 군산).

## 2. 성능 (N2=A)
- **하드 타깃 미설정**. TourAPI 캐시 HIT 시 빠름. 상세는 detailCommon2→detailIntro2 **순차 2콜**(MISS 시) 감수.
- 배지 매칭은 복합 인덱스 조회(소량). 로깅으로 관측만.

## 3. 가용성·신뢰성 (N1=A, U1-a 상속)
- TourAPI 5개 오퍼레이션 각각 Resilience4j **엔드포인트별 서킷/재시도** + 캐시. 실패 → EXTERNAL_API_ERROR(502).
- 상세: detailCommon2 없음 → 404. 배지 미매칭/미적재 → 빈 배열(장애 아님).
- 배지 CSV 적재 실패 격리(행 단위), 기동 지속.

## 4. 보안 [Security Baseline]
- 공개 API(permitAll). 입력 검증: keyword 필수, lat/lng/radius 숫자·양수, region/category enum → 400.
- 민감정보 없음(공개 관광/배지 데이터). serviceKey는 U1-a env 마스킹 상속.

## 5. 유지보수·관측성
- 구조화 로깅 상속. 추가: 배지 매칭 실패 집계(BR-U3-8), businessHours 미매핑 타입 WARN(BR-U3-6), nearby 좌표 제외 카운트.
- 배지 CSV **파일별 인코딩 명시**(모범음식점 UTF-8 / 착한가격 EUC-KR) — 오독 시 정규화 매칭 전부 실패하므로 필수.

## 6. N/A
- 오토스케일링/DR/컴플라이언스 — 현 단계 N/A.
