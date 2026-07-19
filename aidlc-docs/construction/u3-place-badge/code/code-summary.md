# Code Generation Summary — U3 장소·배지 🌐

> **검증**: compileJava/compileTestJava 성공, U3 단위테스트 전부 통과. BadgeCsvLoaderTest는 실제 CSV(모범음식점 UTF-8 52 / 착한가격 EUC-KR 60→요식업 45 = 97건)로 인코딩·요식업 필터·멱등 검증. 전체 빌드/통합(MySQL·TourAPI 실호출)은 Build & Test/부팅 확인.

---

## 1. 생성/수정 파일

### 수정(Modified) — U1-a 확장
- `external/tourapi/TourApiClient.java` — 4 오퍼레이션 추가(searchKeyword2/locationBasedList2/detailCommon2/detailIntro2), 각 @Cacheable+@Retry+@CircuitBreaker(엔드포인트별)+fallback.
- `external/tourapi/TourApiNormalizer.java` — toCommon/toIntro(12·39 분기+미매핑 WARN), homepage href 추출.
- `external/tourapi/dto/TourCommon.java` — contentTypeId 필드 추가(intro 호출·카테고리 매핑용).
- `common/exception/GlobalExceptionHandler.java` — 필수 파라미터 누락/타입 불일치 → 400 핸들러 추가.
- `application.properties` — Resilience4j 인스턴스 4종 추가.
- `src/main/resources/data/` — 배지 CSV 2종 배치(model-restaurants-gunsan UTF-8 / good-price-gunsan EUC-KR).

### 생성(Created) — 앱 코드
| 패키지 | 파일 | 역할 |
|---|---|---|
| place | PlaceController, PlaceService, PlaceDetailAssembler, PlaceMapper | 검색/목록/반경/상세 |
| place.dto | PlaceSummary, PlaceDetail | 응답 DTO |
| badge | BadgeType, Badge(@Entity), BadgeRepository | 배지 영속 |
| badge | BadgeService, AddressNormalizer, BadgeCsvLoader | 매칭·정규화·CSV 적재 |

### 생성(Created) — 테스트
- TourApiNormalizerU3Test, PlaceMapperTest, PlaceDetailAssemblerTest, PlaceControllerTest, AddressNormalizerTest, BadgeServiceTest, BadgeCsvLoaderTest

---

## 2. 스토리 추적 (완료)
| 스토리 | 구현 |
|---|---|
| US-PLACE-1 키워드 검색 | PlaceController.search → PlaceService → searchKeyword2 ✅ |
| US-PLACE-2 지역 목록 | PlaceController.list → areaBasedList2 ✅ |
| US-PLACE-3 반경 주변 | PlaceController.nearby → locationBasedList2(기본 1000m) ✅ |
| US-PLACE-4 장소 상세 | PlaceDetailAssembler common→intro 순차 + 배지/404 ✅ |
| US-BADGE-1 배지 | BadgeCsvLoader(요식업·군산·멱등) + BadgeService(정규화 매칭) ✅ |

---

## 3. 설계 반영 확인
- P2/P3: 상세 = detailCommon2→(contentTypeId)→detailIntro2 순차. P4: nearby 기본 1000m.
- P5: 배지 매칭 = 정규화 업소명 AND 주소 둘 다(보수적). P6: 군산·요식업만(비요식 15 제외).
- P8: businessHours 12(usetime)/39(opentimefood) 분기, 미매핑 타입 null+WARN.
- N1: 엔드포인트별 서킷/캐시 5종. CSV 파일별 인코딩(UTF-8/EUC-KR).
- category 필터: TourAPI엔 coarse contentTypeId 전달 후 정확 CategoryType(CAFE 등) 후처리 필터.

## 4. 기술 메모
- **category→contentTypeId**: CAFE/RESTAURANT 모두 39 → coarse 39로 조회 후 PlaceMapper의 정확 분류(lclsSystm2 FD05)로 후처리 필터.
- **CSV quote-aware 파싱**: 모범음식점 음식종류 필드의 따옴표 내 콤마 보존. BOM 제거.
- **EUC-KR**: 착한가격 CSV는 Charset.forName("EUC-KR")로 읽음(오독 시 매칭 전멸).
- **좌표축**: nearby는 mapX=lng, mapY=lat.

## 5. 로컬 통합 테스트에서 발견·수정한 버그 (실제 TourAPI 부팅 검증)
1. **serviceKey 이중 인코딩(401)**: RestClient UriBuilder가 Encoding 키(%2F 등)를 재인코딩 → 401. `TourApiClient.buildUri`로 URI 직접 조립(serviceKey 원본 유지, 나머지 값만 1회 URL-encode)로 해결. 검증: `/places/search` 실데이터 정상.
2. **Redis 캐시 타입 소실(ClassCastException 500)**: `GenericJacksonJsonRedisSerializer`가 기본 타입정보 미기록 → 캐시 HIT 시 record가 LinkedHashMap으로 복원. `CacheConfig`에서 `enableDefaultTyping`(우리 패키지/컬렉션 범위 제한) 적용. 검증: 동일 상세 2회 호출 정상.
3. **배지 매칭 누락(false negative)**: TourAPI가 상호에 `[착한가게]`/`[모범음식점]` 태그를 붙여 이름 정규화 불일치. `AddressNormalizer`가 대괄호 `[...]`도 제거하도록 확장. 검증: 아서원 → `["GOOD_PRICE"]` 실매칭.
4. **없는 장소 404 대신 400**: `detailCommon`이 null 반환 시 `@Cacheable`(disableCachingNullValues)이 null 캐싱 거부 예외 → 400. `@Cacheable(unless="#result == null")` 추가. 검증: `/places/99999999` → 404.

> 실증 완료(실제 TourAPI+MySQL+Redis): 검색/지역목록/반경(+category 필터)/상세(businessHours)/배지 매칭/404/400 전부 정상. 배지 DB 97건(모범 52+착한 45) 적재.

## 6. 확장 지점 (후속)
- detailInfo2/detailImage2(부가정보/이미지 갤러리) — common과 병렬 확장(TourApiExecutor).
- 배지 하이브리드 매칭·전주 CSV.
- **성능**: 상세/반경은 TourAPI read 3s 타임아웃에 민감(콜드 캐시 시 간헐 502). 캐시 워밍/타임아웃 조정은 운영 시 재검토.
