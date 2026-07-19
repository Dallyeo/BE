# Logical Components — U3 장소·배지 🌐

> 패키지 `com.ppip.dallyeo.place`, `com.ppip.dallyeo.badge`. U1-a TourApiClient 확장.

---

## 1. 컴포넌트 목록

| 컴포넌트 | 유형 | 역할 |
|---|---|---|
| `PlaceController` | @RestController | `/places/search`, `/places`, `/places/nearby`, `/places/{id}` (검증) |
| `PlaceService` | @Service | search/list/nearby 오케스트레이션(TourApiClient→PlaceMapper) |
| `PlaceDetailAssembler` | @Service | 상세 조합: detailCommon→intro 순차 + BadgeService(D2=A) |
| `PlaceMapper` | @Component | TourItem/TourCommon/TourIntro → PlaceSummary/PlaceDetail (좌표/카테고리/businessHours 분기) |
| `TourApiClient`(확장) | @Component | 신규 4 오퍼레이션(search/location/detailCommon/detailIntro) + 기존 area |
| `BadgeController`? | — | (없음 — 배지는 상세 응답에 포함, 독립 엔드포인트 없음) |
| `BadgeService` | @Service | badgesFor(name,address) 정규화 매칭 |
| `BadgeRepository` | JpaRepository<Badge,Long> | 복합 인덱스 조회 |
| `BadgeCsvLoader` | ApplicationRunner | CSV 적재(파일별 인코딩·요식업·군산·멱등) |
| `AddressNormalizer` | @Component | 업소명/주소 보수적 정규화(D1=A) |
| `Badge` | @Entity | 배지(type/region/normName/normAddr, 복합 인덱스) |

---

## 2. 상세 배선

### TourApiClient 확장 (U1-a)
- 신규 메서드 + 어노테이션(엔드포인트별 인스턴스):
  - `searchKeyword2(keyword, lDongCode?, contentTypeId?)` → tourApiSearchKeyword
  - `locationBasedList2(lat,lng,radius,contentTypeId?)` → tourApiLocationBased
  - `detailCommon2(contentId)` → tourApiDetailCommon (contentTypeId 포함 응답)
  - `detailIntro2(contentId, contentTypeId)` → tourApiDetailIntro
- 각 `@Cacheable`+`@Retry`+`@CircuitBreaker(fallback)`. Normalizer(U1-a TourApiNormalizer) 재사용/확장.

### PlaceService
- search/listByRegion/nearby: TourApiClient 호출 → PlaceMapper → PlaceSummary[]. region→LDongCode(U1-a RegionCatalog).

### PlaceDetailAssembler (D2=A)
- assemble(id): common → (typeId) → intro → PlaceMapper.toDetail(common,intro) → badges=BadgeService.badgesFor(name,address) → PlaceDetail. common 없음 → 404.

### PlaceMapper
- toSummary(TourItem): 좌표/카테고리/thumbnail. 엔드포인트별 좌표 정책은 TourApiNormalizer(U1-a) 처리.
- toDetail(TourCommon,TourIntro): businessHours = typeId 12(usetime)/39(opentimefood) 분기, 그 외 null+WARN(BR-U3-6).

### Badge 계층
- `Badge`(@Entity, @Table indexes (normalizedName, normalizedAddress)).
- `BadgeRepository.findByNormalizedNameAndNormalizedAddress`.
- `BadgeService.badgesFor(name,address)`: AddressNormalizer로 정규화 → 조회 → List<String>(BadgeType.name()).
- `BadgeCsvLoader`: 모범음식점(UTF-8)→MODEL_RESTAURANT, 착한가격(EUC-KR)→요식업만 GOOD_PRICE. 멱등 저장.
- `AddressNormalizer`: normalizeName/normalizeAddress(보수적, D1=A).

---

## 3. 조회 흐름
```
/places/search|nearby|list → PlaceController(검증) → PlaceService → TourApiClient(캐시/회복성) → PlaceMapper → [PlaceSummary]
/places/{id} → PlaceController → PlaceDetailAssembler → detailCommon2 →(typeId)→ detailIntro2 → PlaceMapper
                                           → BadgeService.badgesFor(정규화 매칭) → PlaceDetail
기동 → BadgeCsvLoader → badge 테이블(정규화 키, 복합 인덱스)
```

## 4. U3 경계
- 신규 소유: `badge` 테이블, place/badge 컴포넌트, TourApiClient 4 오퍼레이션 확장.
- 향후: detailInfo2/detailImage2, 배지 하이브리드/전주.
