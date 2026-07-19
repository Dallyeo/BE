# NFR Requirements Plan — U3 장소·배지 🌐

U3 비기능 요구 확정. U3는 U1-a TourAPI 회복성/캐시를 본격 확장 + 배지 DB 추가. 대부분 U1-a/U2 상속.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **상속**: U1-a(Resilience4j 엔드포인트별 서킷·Redis 캐시 TTL30분+·구조화 로깅·permitAll 공개), U2(JPA/MySQL/인덱스/멱등 시드).

---

## Part A. 실행 체크리스트
- [x] FD 분석
- [x] 결정 질문 수집(Part B)
- [x] `nfr-requirements.md`
- [x] `tech-stack-decisions.md`

---

## Part B. 결정 질문

### Question N1 — TourAPI 신규 오퍼레이션 회복성/캐시
search/list/nearby/detailCommon/detailIntro 5개 오퍼레이션에 U1-a 패턴을 확장할까요?

A) **엔드포인트별 서킷 인스턴스 + @Cacheable 각각 적용** — U1-a 명명규칙(tourApiSearchKeyword 등) 그대로, TTL 30분+ (권장)
B) 단일 공유 서킷 + 캐시
X) Other

[Answer]: A

---

### Question N2 — 상세 조합 성능
`/places/{id}`는 detailCommon2 → detailIntro2 순차 2콜입니다. 성능 목표는?

A) **하드 타깃 미설정(상속)** — 캐시 HIT 시 빠름, MISS는 2콜 순차 감수. 로깅으로 관측 (권장)
B) 상세 응답 시간 정량 목표 설정
X) Other

[Answer]: A

---

### Question N3 — 배지 조회/적재 성능
배지 매칭(정규화 업소명+주소)과 CSV 적재는?

A) **(normalizedName, normalizedAddress) 복합 인덱스 + 기동 멱등 적재** — 소량(≤112건) DB 조회 (권장)
B) 인메모리 맵 캐싱(기동 시 배지 전체 메모리 로드 후 매칭)
X) Other

[Answer]: A
배지 CSV는 파일별 인코딩이 다르므로(모범음식점 UTF-8, 착한가격업소 EUC-KR)
BadgeCsvLoader가 파일별 인코딩을 명시적으로 지정해 읽도록 tech-stack-decisions.md에 명시.
(EUC-KR을 UTF-8로 읽으면 업소명이 깨져 정규화 매칭이 전부 실패함)
---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u3-place-badge/nfr-requirements/nfr-requirements.md`
- [x] `aidlc-docs/construction/u3-place-badge/nfr-requirements/tech-stack-decisions.md`
