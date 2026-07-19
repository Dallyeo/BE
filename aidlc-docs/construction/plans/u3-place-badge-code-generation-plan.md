# Code Generation Plan — U3 장소·배지 🌐

> **U3 Code Generation의 단일 진실 소스**. Part 2에서 순서대로 실행, 각 단계 완료 시 즉시 `[x]`.
> Brownfield / Spring Boot 4 / Java 17. Base package `com.ppip.dallyeo`.

---

## 유닛 컨텍스트

### 구현 스토리
- **US-PLACE-1** 키워드 검색(`GET /places/search`)
- **US-PLACE-2** 지역 목록(`GET /places?region&category`)
- **US-PLACE-3** 반경 주변(`GET /places/nearby?lat&lng&radius&category`)
- **US-PLACE-4** 장소 상세(`GET /places/{id}`)
- **US-BADGE-1** 모범음식점/착한가격 배지

### 의존/경계
- 의존: U1-a(TourApiClient·TourApiNormalizer·TourItem/TourCommon/TourIntro·CategoryMapper·RegionCatalog·ApiResponse·예외·LogMaskingUtil). 인증 불필요.
- 소유: `badge` 테이블. TourApiClient는 U1-a 것을 **오퍼레이션 추가로 확장**(in-place).
- 설계: FD(business-logic-model/rules/entities), NFR(엔드포인트별 서킷+캐시, 배지 복합인덱스, CSV 파일별 인코딩), Infra(badge 테이블, CSV classpath, TourAPI egress).

### 패키지 구조
```
com.ppip.dallyeo.place/
   PlaceController, PlaceService, PlaceDetailAssembler, PlaceMapper
   dto/  PlaceSummary, PlaceDetail
com.ppip.dallyeo.badge/
   Badge(@Entity), BadgeType(enum), BadgeRepository, BadgeService, AddressNormalizer, BadgeCsvLoader
com.ppip.dallyeo.external.tourapi/  (U1-a 확장)
   TourApiClient(+searchKeyword2/locationBasedList2/detailCommon2/detailIntro2)
   TourApiNormalizer(+toCommon/toIntro)
```

### CSV 구조 (검증됨)
- 모범음식점(UTF-8, BOM): `연번,업소명,소재지,연락처,음식종류` → name=업소명(1), addr=소재지(2). 전건 MODEL_RESTAURANT.
- 착한가격(EUC-KR): `연번,업종,업소명,주소(도로명)` → 업종(1) 요식업만, name=업소명(2), addr=주소(3). 제외: 미용업/목욕업/이용업.

---

## 실행 단계 (Part 2)

### Step 1 — 설정/리소스  [NFR]
- [x] `application.properties`: Resilience4j 인스턴스 4종(tourApiSearchKeyword/tourApiLocationBased/tourApiDetailCommon/tourApiDetailIntro, base-config=default) 추가.
- [x] CSV 2종 → `src/main/resources/data/` 복사.

### Step 2 — TourApiNormalizer 확장  [US-PLACE-4] (BR-U3-6)
- [x] `TourApiNormalizer`에 `toCommon(JsonNode)`→TourCommon, `toIntro(JsonNode, contentTypeId)`→TourIntro(12 usetime/39 opentimefood 분기, 그 외 null) 추가.
- [x] `TourApiNormalizerU3Test` — common/intro 파싱, 타입 분기.

### Step 3 — TourApiClient 확장  [US-PLACE-1/2/3/4] (BR-U3-11)
- [x] `searchKeyword2(keyword, lDongCode?, contentTypeId?, page, rows)`, `locationBasedList2(lat,lng,radius,contentTypeId?,page,rows)`, `detailCommon2(contentId)`, `detailIntro2(contentId, contentTypeId)` 추가.
- [x] 각 @Cacheable + @Retry + @CircuitBreaker(엔드포인트별, fallback) + LogMasking. area는 기존 재사용.

### Step 4 — Place DTO  [US-PLACE-1~4]
- [x] `place/dto/PlaceSummary`(id,name,category,lat,lng,address,thumbnailUrl,distanceMeters), `PlaceDetail`(+businessHours,imageUrl,badges).

### Step 5 — PlaceMapper  [US-PLACE-1~4] (BR-U3-1/6)
- [x] `place/PlaceMapper`: toSummary(TourItem), toDetail(TourCommon,TourIntro,badges). businessHours 12/39 분기+null 사유 구분 WARN.
- [x] `PlaceMapperTest`.

### Step 6 — PlaceService  [US-PLACE-1/2/3]
- [x] `place/PlaceService`: search/listByRegion/nearby → TourApiClient + Normalizer + PlaceMapper. region→LDongCode(RegionCatalog).

### Step 7 — Badge 도메인  [US-BADGE-1] (BR-U3-8/9)
- [x] `badge/BadgeType`(MODEL_RESTAURANT,GOOD_PRICE), `badge/Badge`(@Entity, @Table index (normalizedName,normalizedAddress)), `badge/BadgeRepository`(findByNormalizedNameAndNormalizedAddress).

### Step 8 — AddressNormalizer  [US-BADGE-1] (BR-U3-9, D1)
- [x] `badge/AddressNormalizer`: normalizeName/normalizeAddress(공백/괄호내용/특수문자 제거, 시도명 통일).
- [x] `AddressNormalizerTest`.

### Step 9 — BadgeService  [US-BADGE-1] (BR-U3-8)
- [x] `badge/BadgeService.badgesFor(name,address)`: 정규화(둘 다)→조회→List<String>. 미매칭 []. 매칭 실패 집계 로그.
- [x] `BadgeServiceTest`(Mockito).

### Step 10 — BadgeCsvLoader  [US-BADGE-1] (BR-U3-10)
- [x] `badge/BadgeCsvLoader`(ApplicationRunner): 모범음식점(UTF-8)→MODEL_RESTAURANT, 착한가격(EUC-KR)→요식업만 GOOD_PRICE. quote-aware 파싱. (type,normName,normAddr) 멱등. 결과/제외 로그.
- [x] `BadgeCsvLoaderTest` — 실제 CSV로 적재 건수/요식업 필터/멱등.

### Step 11 — PlaceDetailAssembler  [US-PLACE-4] (BR-U3-5, D2)
- [x] `place/PlaceDetailAssembler.assemble(id)`: detailCommon2→(typeId)→detailIntro2→PlaceMapper→BadgeService 부착. common 없음 404.
- [x] `PlaceDetailAssemblerTest`(Mockito).

### Step 12 — PlaceController  [US-PLACE-1~4] (BR-U3-4)
- [x] `place/PlaceController`: search/list/nearby/{id}. 검증(keyword 필수, lat/lng 숫자, radius 양수 기본 1000, region/category enum → 400).
- [x] `PlaceControllerTest`(Mockito, 검증 400).

### Step 13 — 빌드/컴파일 검증
- [x] `./gradlew compileJava compileTestJava` + 인프라 불필요 단위테스트 스모크.

### Step 14 — 코드 요약 문서
- [x] `aidlc-docs/construction/u3-place-badge/code/code-summary.md`.

---

## 산출물 위치
- 앱 코드: `src/main/java/com/ppip/dallyeo/{place,badge,external/tourapi}/...`, 리소스 `src/main/resources/data/*.csv`
- 문서: `aidlc-docs/construction/u3-place-badge/code/`

## 범위 밖 / 이관
- detailInfo2/detailImage2(부가/갤러리) — 후속(P2)
- 배지 하이브리드 매칭·전주 CSV — 후속
- 인증 — 공개 유닛(해당 없음)
