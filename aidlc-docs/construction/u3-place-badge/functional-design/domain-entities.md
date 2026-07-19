# Domain Entities / DTOs — U3 장소·배지 🌐

> 확정: P1=A(4엔드포인트), P2=A(common+intro), P3=A(순차 common→intro), P4=A(기본 1000m), P5=A(정규화 exact·업소명+주소 둘 다·보수적), P6=A(군산·요식업만), P7=A(캐싱), P8=A(12/39 분기·null 사유 구분).
> U1-a 재사용: TourApiClient, TourItem/TourCommon/TourIntro DTO, CategoryType/PlaceCategory, CategoryMapper, Region/RegionCatalog, ApiResponse, BusinessException.

---

## 1. 응답 DTO (Place)

### PlaceSummary — 목록(search/list/nearby)
```
PlaceSummary
  - String id            // TourAPI contentId
  - String name          // title
  - CategoryType category
  - Double latitude      // mapy (null 가능: area/keyword)
  - Double longitude     // mapx
  - String address       // addr1(+addr2)
  - String thumbnailUrl  // firstimage2 (빈값 null)
  - Double distanceMeters// nearby 전용 (dist), 그 외 null
```

### PlaceDetail — 상세(/places/{id})
```
PlaceDetail
  - String id, name
  - CategoryType category
  - Double latitude, longitude
  - String address
  - String businessHours // intro (12 usetime / 39 opentimefood), 그 외 null (P8)
  - String imageUrl      // common firstimage (빈값 null)
  - List<String> badges  // ["MODEL_RESTAURANT","GOOD_PRICE"], 미매칭 시 []
```
> §6.2 스펙 필드. detailInfo2/detailImage2(부가/갤러리)는 이번 범위 밖(P2=A) — 확장 지점.

---

## 2. 영속 엔티티 (Badge)

### Badge (테이블 `badge`) — US-BADGE-1
```
Badge
  - Long id                @Id @GeneratedValue
  - BadgeType type         @Enumerated(STRING)  // MODEL_RESTAURANT | GOOD_PRICE
  - Region region          @Enumerated(STRING)  // 현재 GUNSAN만 적재(P6)
  - String placeName       // 원본 업소명
  - String normalizedName  // 정규화 업소명 (매칭 키)
  - String normalizedAddress // 정규화 주소 (매칭 키)
  인덱스: (normalizedName, normalizedAddress)  // 매칭 조회
```
> 소유 유닛: U3. CSV 적재 결과 저장. 매칭은 **normalizedName AND normalizedAddress 둘 다 일치**(P5).

```
enum BadgeType { MODEL_RESTAURANT, GOOD_PRICE }
```

---

## 3. 검색/필터 파라미터 (요청 모델)

```
PlaceSearchParams (search)   : keyword(필수), region?(GUNSAN/JEONJU), category?(PlaceCategory)
PlaceListParams   (list)     : region(필수), category?
PlaceNearbyParams (nearby)   : lat(필수), lng(필수), radius?(기본 1000, P4), category?
```
- 검증: 필수값 누락/잘못된 enum → 400(BR-U3). region→RegionCatalog LDongCode 변환(U1-a).

---

## 4. TourAPI 조합 모델 (내부)
- 목록: `TourItem`(U1-a) → PlaceMapper → PlaceSummary.
- 상세: `TourCommon`(U1-a) + `TourIntro`(U1-a) → PlaceMapper → PlaceDetail.
  - detailCommon2로 contentTypeId 확보 → 그 typeId로 detailIntro2 호출(P3 순차).
- 배지: BadgeService가 (normalizedName, normalizedAddress)로 badge 테이블 조회 → PlaceDetail.badges.

---

## 5. 신규 TourApiClient 오퍼레이션 (U1-a 확장)
U1-a는 areaBasedList2만 골격. U3에서 아래 추가(엔드포인트별 서킷 인스턴스, P7):
```
locationBasedList2 (nearby)   → tourApiLocationBased
searchKeyword2     (search)   → tourApiSearchKeyword
areaBasedList2     (list)     → tourApiAreaBased (U1-a 기존)
detailCommon2      (상세개요)  → tourApiDetailCommon
detailIntro2       (상세소개)  → tourApiDetailIntro
```
각 @Cacheable + @Retry + @CircuitBreaker + fallback→ExternalApiException.
