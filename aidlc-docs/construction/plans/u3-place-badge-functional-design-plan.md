# Functional Design Plan — U3 장소·배지 🌐

U3의 비즈니스 로직/도메인/규칙 확정을 위한 **계획 + 질문**. U3는 U1-a `TourApiClient`를 본격 활용하는 **큰 유닛**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **스토리**: US-PLACE-1(키워드 검색), US-PLACE-2(지역/현위치 목록), US-PLACE-3(반경 주변), US-PLACE-4(장소 상세), US-BADGE-1(모범음식점/착한가격 배지).
> **컴포넌트**: PlaceController/PlaceService/PlaceDetailAssembler/PlaceMapper, BadgeService/BadgeRepository/BadgeCsvLoader/AddressNormalizer.
> **의존**: U1-a(TourApiClient·캐시·회복성·ApiResponse·CategoryMapper·RegionCatalog). 인증 불필요.
> **API(api-spec §6)**: `GET /places/search`(키워드), `GET /places`(지역목록), `GET /places/nearby`(반경), `GET /places/{id}`(상세). 목록 필드: id/name/category/lat/lng/address/thumbnailUrl(+반경 시 distanceMeters). 상세: +businessHours/imageUrl/**badges[]**.
> **TourAPI 매핑**: 목록=areaBasedList2/locationBasedList2/searchKeyword2(동일 필드), 상세=detailCommon2(+detailIntro2 contentTypeId별 분기 +detailInfo2 +detailImage2).
> **배지**: 군산 모범음식점(UTF-8 52)·착한가격업소(EUC-KR 60). lcnsno 없음 → **업소명+주소 정규화 매칭**. 확정: 요식업만·군산만.

---

## Part A. 실행 체크리스트
- [x] 유닛 컨텍스트 분석
- [x] 결정 질문 수집(Part B)
- [x] `business-logic-model.md` — 검색/목록/반경/상세조합/배지매칭 흐름
- [x] `business-rules.md` — 매핑/좌표정책/카테고리/배지/캐시 규칙
- [x] `domain-entities.md` — Place DTO, Badge 엔티티, TourAPI 조합 모델

---

## Part B. 결정 질문

### Question P1 — U3 엔드포인트 범위
4개 엔드포인트를 모두 U3에서 구현할까요?

A) **4개 모두** — /places/search, /places(지역목록), /places/nearby(반경), /places/{id}(상세) (권장, 스토리 US-PLACE-1~4 대응)
B) 검색/상세만 우선(목록/반경은 후속)
X) Other

[Answer]: A

---

### Question P2 — 장소 상세 조합 콜 범위
상세(`/places/{id}`) 응답 필드는 name/category/lat/lng/address/businessHours/imageUrl/badges. 어떤 TourAPI 상세 콜을 조합할까요?

A) **detailCommon2 + detailIntro2**(2콜) — 응답 필드에 필요한 최소 조합(개요+영업시간). imageUrl은 common의 firstimage (권장, 단순·응답스펙 충족)
B) **4콜 전부**(common+intro+info+image) — 부가정보/이미지 갤러리까지(응답 확장 시)
C) 위임
X) Other

[Answer]: A

---

### Question P3 — 상세 조회 시 contentTypeId 확보
detailIntro2는 `contentTypeId`가 필요한데 `/places/{id}`는 id(contentid)만 받습니다. 어떻게 확보?

A) **detailCommon2 먼저 호출 → contentTypeId 획득 → detailIntro2 호출**(2단계, intro는 typeId 확정 후) (권장, 클라 부담 없음)
B) **클라이언트가 `?contentTypeId=` 전달** — 목록 응답의 category/typeId를 재사용(1홉 절약, 프론트 계약 변경)
C) 위임
X) Other

[Answer]: A
detailIntro2는 detailCommon2의 contentTypeId에 의존하므로 순차 호출.
business-logic-model.md에 "향후 detailInfo2/detailImage2 추가 시
common과 병렬, intro만 common 뒤에 순차" 구조로 명시.
---

### Question P4 — nearby 기본 반경(radius)
`/places/nearby`의 기본 반경 기본값은? (api-spec 미확정: 먹거리 1km vs 완주결과 500m)

A) **기본 1000m**(파라미터 없으면 1km), 클라가 radius로 조정 (권장)
B) 기본 500m
C) **radius 필수**(기본값 없음, 미지정 시 400)
X) Other

[Answer]: A

---

### Question P5 — 배지 매칭 구현 방식
lcnsno 없음 → 업소명+주소 정규화 매칭. 구현 수준은? (분석서 결론: D 하이브리드)

A) **정규화 후 정확일치(자동)** — AddressNormalizer로 업소명+주소 정규화 후 exact match. 미매칭은 배지 없음(빈 배열). 단순·자동, 일부 미스 감수 (권장, 1차 구현)
B) **수동 매핑 테이블(C)** — 112건 사람 매핑표 작성 후 적재(정확하나 수작업)
C) **하이브리드(D)** — 자동 후보 + 애매건 수동 확정(정확도↑, 공수↑)
X) Other

[Answer]: A
배지 매칭 규칙을 business-rules.md에 명시:
- 업소명 + 주소 "둘 다" 정규화 일치 시에만 매칭 (한쪽만으로는 매칭 금지)
- 정규화는 보수적으로 적용 (오매칭이 미매칭보다 위험하므로 애매하면 매칭 안 함)
- 매칭 실패 건수를 로그로 집계 (향후 하이브리드(C) 전환 판단 근거)
---

### Question P6 — 배지 적재 범위 (확정 확인)
분석서 확정 사항을 그대로 반영할까요?

A) **군산만 + 요식업만 적재** — 착한가격 비음식(미용/목욕/이용 15건) 제외, 전주는 추후 (권장, 분석서 확정)
B) 전체 적재(비음식 포함, 매칭 안 되면 자연 제외)
X) Other

[Answer]: A

---

### Question P7 — TourAPI 캐싱 (U1-a 재사용)
장소 검색/목록/상세의 TourAPI 호출을 U1-a Redis 캐시(@Cacheable, TTL 30분+)로 캐싱할까요?

A) **캐싱함** — 목록/상세 각 오퍼레이션 캐시(U1-a 패턴 재사용, 엔드포인트별 서킷 인스턴스도 추가). 부하/레이트리밋 완화 (권장)
B) 캐싱 안 함(실시간 우선)
X) Other

[Answer]: A

---

### Question P8 — intro contentTypeId 분기 처리
detailIntro2는 타입별 필드가 다름(관광지12 `usetime` vs 음식점39 `opentimefood`). businessHours를 어떻게?

A) **12/39 분기 매핑 + 그 외 타입은 businessHours=null** — 확보된 두 타입만 정규화, 미지원 타입은 빈값 (권장)
B) 모든 타입 필드 시도(가능한 필드 순차 탐색)
X) Other

[Answer]: A
businessHours=null 사유를 구분:
- 타입 12/39인데 TourAPI 값 없음 → 정상 null
- 미매핑 타입이라 시도 안 함 → WARN 로그
  business-rules.md에 두 경우를 구분해 명시.
---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u3-place-badge/functional-design/business-logic-model.md`
- [x] `aidlc-docs/construction/u3-place-badge/functional-design/business-rules.md`
- [x] `aidlc-docs/construction/u3-place-badge/functional-design/domain-entities.md`
