# Business Logic Model — U3 장소·배지 🌐

> 장소 검색/목록/반경/상세(TourAPI 조합) + 배지 매칭. U1-a TourApiClient/캐시/회복성 활용. 응답은 ApiResponse 래핑.

---

## 1. 키워드 검색 (US-PLACE-1) — `GET /places/search`
```
PlaceController.search(keyword, region?, category?)
  검증(keyword 필수, region/category enum)
  → PlaceService.search(...)
       → TourApiClient.searchKeyword2(keyword, lDongCode?, contentTypeId?)  [캐시/회복성]
       → PlaceMapper: TourItem[] → PlaceSummary[] (좌표 null 유지, 카테고리 매핑)
  → ApiResponse.success([PlaceSummary])
```

## 2. 지역 장소 목록 (US-PLACE-2) — `GET /places?region&category`
```
PlaceController.list(region 필수, category?)
  region → RegionCatalog.LDongCode (U1-a)
  → PlaceService.listByRegion(...)
       → TourApiClient.areaBasedList2(lDongCode, contentTypeId?)  [캐시/회복성]
       → PlaceMapper → PlaceSummary[]  (좌표 null 유지)
```

## 3. 반경 주변 (US-PLACE-3) — `GET /places/nearby?lat&lng&radius&category`
```
PlaceController.nearby(lat 필수, lng 필수, radius=1000 기본(P4), category?)
  → PlaceService.nearby(...)
       → TourApiClient.locationBasedList2(lat,lng,radius,contentTypeId?)  [캐시/회복성]
       → PlaceMapper → PlaceSummary[]  (좌표 필수 → 파싱 실패 항목 제외 BR-3.3, distanceMeters 포함)
```

## 4. 장소 상세 (US-PLACE-4) — `GET /places/{id}`
```
PlaceController.detail(id)
  → PlaceDetailAssembler.assemble(id)
       1) TourApiClient.detailCommon2(id)  → TourCommon (+ contentTypeId 확보)  [캐시/회복성]
            없음/빈 응답 → 404 NOT_FOUND
       2) TourApiClient.detailIntro2(id, contentTypeId)  → TourIntro  [캐시/회복성]
            (P3: intro는 common의 contentTypeId에 의존 → 순차 호출)
       3) PlaceMapper: (common+intro) → PlaceDetail
            businessHours = intro 타입별 매핑(12 usetime / 39 opentimefood), 그 외 null+WARN(P8)
       4) BadgeService.badgesFor(name, address) → PlaceDetail.badges
  → ApiResponse.success(PlaceDetail)
```
> **향후 확장 구조(P3 메모)**: detailInfo2/detailImage2 추가 시 — common과 **병렬** 호출 가능하나, **intro만 common(=contentTypeId) 뒤에 순차**. 즉 `common → [intro 순차] + [info/image 병렬]`. 이번엔 common+intro만.

---

## 5. 배지 매칭 흐름 (US-BADGE-1)
```
BadgeService.badgesFor(placeName, address)
  normName = AddressNormalizer.normalizeName(placeName)
  normAddr = AddressNormalizer.normalizeAddress(address)
  → BadgeRepository.findByNormalizedNameAndNormalizedAddress(normName, normAddr)
  → 매칭된 BadgeType 목록 반환 (없으면 [] )
```
- **업소명 AND 주소 둘 다 일치**해야 매칭(P5). 한쪽만으로는 매칭 금지(오매칭 방지).
- 보수적 정규화(애매하면 매칭 안 함 → 미매칭이 오매칭보다 안전).

## 6. 배지 CSV 적재 흐름 (BadgeCsvLoader) — 기동 시
```
앱 기동 (ApplicationRunner)
  1) 모범음식점 CSV(UTF-8) 로드 → 각 행: 업소명/소재지 → 정규화 → Badge(MODEL_RESTAURANT, GUNSAN) upsert
  2) 착한가격업소 CSV(EUC-KR→UTF-8 변환) 로드 → 업종이 요식업인 행만(P6) → Badge(GOOD_PRICE, GUNSAN)
  3) 멱등: (type,normName,normAddr) 기준 존재 시 스킵
  4) 적재 결과 INFO 로그 (건수), 매칭 실패/제외 카운트 로그
```
- 인코딩: 모범음식점 UTF-8, 착한가격 **EUC-KR** → 읽을 때 charset 지정.
- 비요식업(미용/목욕/이용) 제외(P6).

---

## 7. 데이터/캐시 흐름 요약
```
GET /places/search|nearby|list → PlaceService → TourApiClient(캐시/회복성) → PlaceMapper → [PlaceSummary]
GET /places/{id} → Assembler → detailCommon2 →(typeId)→ detailIntro2 → PlaceMapper + BadgeService → PlaceDetail
기동 → BadgeCsvLoader → badge 테이블(정규화 키)
```
- TourAPI 호출은 전부 U1-a Redis 캐시(TTL 30분+) + Resilience4j(P7). 실패 → EXTERNAL_API_ERROR(502).
