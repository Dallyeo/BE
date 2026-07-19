# Business Rules — U3 장소·배지 🌐

> 장소 매핑/좌표/카테고리/상세조합/배지/캐시 규칙. U1-a BR-3(정규화)/BR-4(카테고리)/BR-6(캐시)/BR-7(회복성) 상속.

---

## BR-U3-1. 장소 목록 매핑 (TourItem → PlaceSummary)
- id=contentId, name=title, category=CategoryMapper(contentTypeId,lclsSystm2), latitude=mapy, longitude=mapx, address=addr1(+addr2), thumbnailUrl=firstimage2(빈값→null).
- 빈 문자열 → null (U1-a BR-3.1).

## BR-U3-2. 좌표 정책 (엔드포인트별, U1-a BR-3.3 상속)
- `searchKeyword2`/`areaBasedList2`: 좌표 파싱 실패 시 **null 유지, 항목 보존**.
- `locationBasedList2`(nearby): 좌표 **필수 → 실패 항목 제외** + distanceMeters(dist) 포함. 제외 카운트 로그.

## BR-U3-3. 카테고리 매핑 (U1-a BR-4 상속)
- CategoryMapper 재사용: contentTypeId(+lclsSystm2 FD05→CAFE) → CategoryType, 미매핑 ETC + WARN.

## BR-U3-4. 입력 검증 (400)
- search: `keyword` 필수(공백 불가). region/category 잘못된 enum → 400.
- list: `region` 필수. nearby: `lat`/`lng` 필수(숫자), radius 양수(기본 1000, P4), 음수/0 → 400.
- 잘못된 값 → BAD_REQUEST/VALIDATION_ERROR (U1-a 예외 체계).

## BR-U3-5. 상세 조합 (P2/P3)
- `/places/{id}` = detailCommon2 + detailIntro2 (2콜).
- **contentTypeId는 detailCommon2 응답에서 확보 → detailIntro2 순차 호출**(P3). common 없으면 그 자리에서 404.
- imageUrl = common.firstimage(빈값 null). businessHours = intro(BR-U3-6).
- 향후 detailInfo2/detailImage2 확장 시: common과 병렬 가능, intro만 common 뒤 순차(구조 유지).

## BR-U3-6. businessHours 타입 분기 (P8)
- contentTypeId **12(관광지)** → `usetime`, **39(음식점)** → `opentimefood` 를 businessHours로 정규화.
- **null 사유 구분**:
  - 타입 12/39인데 TourAPI 값이 비어있음 → **정상 null**(로그 없음).
  - 12/39 외 미지원 타입이라 매핑 시도 안 함 → businessHours=null + **WARN 로그**(신규 타입 인지).

## BR-U3-7. 상세 Not Found
- detailCommon2가 항목 없음/빈 응답 → 404 NOT_FOUND (BusinessException).

## BR-U3-8. 배지 매칭 (P5)
- 매칭 키: **정규화 업소명 AND 정규화 주소 둘 다 일치**. 한쪽만 일치 → 매칭 안 함(오매칭 방지).
- 보수적 정규화: 애매하면 매칭하지 않음(미매칭 > 오매칭).
- 미매칭/미적재 → `badges: []`.
- **매칭 실패(장소에 배지 후보 없음이 아니라, 정규화 불일치) 건수를 로그 집계** → 향후 하이브리드(C) 전환 판단 근거.

## BR-U3-9. 주소/업소명 정규화 (AddressNormalizer)
- 공백/괄호(동명 등)/특수문자 제거, 대소문자·전각 정규화. 업소명·주소 각각 규칙 적용.
- 도로명 주소 우선. (좌표·인허가번호 없음 → 텍스트 정규화만 가능.)

## BR-U3-10. 배지 CSV 적재 (P6)
- **군산만 + 요식업만**. 모범음식점(UTF-8)=MODEL_RESTAURANT 전건, 착한가격업소(EUC-KR→UTF-8)=요식업 업종만 GOOD_PRICE(미용/목욕/이용 제외).
- 멱등: (type, normName, normAddr) 존재 시 스킵. 결과/제외 건수 INFO 로그.
- 인코딩: 착한가격 CSV는 **EUC-KR**로 읽어 UTF-8 처리.

## BR-U3-11. 캐싱·회복성 (P7, U1-a 상속)
- searchKeyword2/areaBasedList2/locationBasedList2/detailCommon2/detailIntro2 각각 @Cacheable(TTL 30분+) + @Retry + @CircuitBreaker(엔드포인트별 인스턴스) + fallback→EXTERNAL_API_ERROR(502).
- 캐시 키: 오퍼레이션 + 정규화 파라미터(U1-a BR-6).

## BR-U3-12. 응답 래핑 (U1-a 일관)
- 성공 ApiResponse.success(data), 실패 GlobalExceptionHandler. 빈 목록도 200 + [].
