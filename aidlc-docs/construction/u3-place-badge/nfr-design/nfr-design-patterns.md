# NFR Design Patterns — U3 장소·배지 🌐

> 확정: D1=A(보수적 정규화), D2=A(Assembler가 BadgeService 호출). 회복성/캐시는 U1-a 상속·확장.

---

## 1. 회복성/캐시 패턴 (N1, U1-a 상속·확장)
- TourApiClient 신규 5 오퍼레이션에 U1-a 패턴 그대로: `@Cacheable`(TTL 30분+) + `@Retry` + `@CircuitBreaker`(엔드포인트별 인스턴스) + fallback→ExternalApiException.
- 인스턴스: `tourApiSearchKeyword`, `tourApiLocationBased`, `tourApiAreaBased`(기존), `tourApiDetailCommon`, `tourApiDetailIntro` (application.properties base-config=default).
- 캐시 우선순위: `@EnableCaching(HIGHEST_PRECEDENCE)`(U1-a) — HIT 시 외부/회복성 스킵.

## 2. 상세 조합 패턴 (P3)
- `PlaceDetailAssembler`: detailCommon2 호출 → contentTypeId 확보 → detailIntro2 순차 호출 → PlaceMapper 조합 → BadgeService로 badges 부착(D2=A).
- common 없음 → 404. intro 실패는 회복성 폴백(부분 실패 시 상세 전체 502) — 단순화: 각 콜 캐시/회복성 개별 적용.
- **향후 확장**: detailInfo2/detailImage2 추가 시 common과 병렬(TourApiExecutor, U1-a D6), intro만 common 뒤 순차.

## 3. 정규화 패턴 (D1=A 보수적)
- `AddressNormalizer`:
  - 공백 전부 제거, 괄호 `()`와 내부 내용(동명 등) 제거, 특수문자(`-·,.`) 제거.
  - 시도명 표기 통일: `전북특별자치도`=`전라북도`=`전북` → 하나로.
  - 그 이상(도로명 파싱/지점명 분리) 안 함 → 오매칭 방지.
- 업소명/주소 각각 규칙 적용, 매칭은 **둘 다 일치**(P5).

## 4. 배지 조회/적재 패턴 (N3)
- 조회: `BadgeRepository.findByNormalizedNameAndNormalizedAddress` (복합 인덱스). 다건이면 BadgeType 집합.
- 적재: `BadgeCsvLoader`(ApplicationRunner) — 파일별 Charset(UTF-8/EUC-KR) 명시, 요식업·군산만, (type,normName,normAddr) 멱등, 결과/제외 카운트 로그.

## 5. 검증 패턴 (보안)
- 컨트롤러 파라미터 검증: keyword 필수, lat/lng 숫자, radius 양수(기본 1000), region/category enum → 400.

## 6. 관측성
- 배지 매칭 실패 집계 로그(BR-U3-8), businessHours 미매핑 타입 WARN(BR-U3-6), nearby 좌표 제외 카운트(BR-U3-2), CSV 적재 결과.

## 7. N/A
- 신규 인프라·큐·벌크헤드(현 단계) — TourApiExecutor는 U1-a 존재, U3 상세 병렬은 향후.
