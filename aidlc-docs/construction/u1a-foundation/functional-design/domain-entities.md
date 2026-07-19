# Domain Entities / Value Objects — U1-a 공개 기반

> 기반 유닛은 영속 엔티티가 거의 없음. 주로 **값 객체(VO)/DTO/enum**. (실제 필드/애노테이션은 Code Generation에서 구현)

---

## 1. 공통 응답 (VO)
```
ApiResponse<T>
  - boolean success
  - T data            // 성공 시
  - ApiError error    // 실패 시 (success=false)
  정적 팩토리: success(T), success(), failure(ApiError)

ApiError
  - String code                 // 문자열 도메인 코드 (BR-1)
  - String message
  - List<ErrorDetail> details   // 선택 (검증 실패 시)

ErrorDetail
  - String field
  - String message
```

## 2. TourAPI DTO (외부 응답 매핑용, 내부 정규화 전/후)
```
TourItem        // 목록(area/location/keyword 공통)
  - String contentId, title
  - Double latitude, longitude   // mapy, mapx 파싱 (null 가능: area/keyword)
  - Double distanceMeters        // dist (location 전용)
  - String address               // addr1(+addr2)
  - String thumbnailUrl          // firstimage2 (빈값→null)
  - int contentTypeId
  - String lclsSystm1, lclsSystm2, lclsSystm3

TourCommon      // detailCommon2
  - String contentId, title, overview, homepage, imageUrl, tel, address
  - Double latitude, longitude

TourIntro       // detailIntro2 (타입별 필드 → 공통 정규화 필드)
  - String businessHours, restDate, parking, inquiry
  - (관광지/음식점 원본 필드는 매핑 단계에서 흡수)

TourInfo        // detailInfo2 반복 항목
  - String name (infoname), text (infotext)

TourImage       // detailImage2
  - String url (originimgurl), thumbnailUrl (smallimageurl), name (imgname)
```
> 정규화 규칙: 빈 문자열→null, 좌표 파싱, 엔드포인트별 좌표 실패 정책(BR-3).

## 3. 카테고리 (VO/enum)
```
enum CategoryType
  TOUR, RESTAURANT, CAFE, CULTURE, FESTIVAL, TRAVEL_COURSE,
  LEPORTS, STAY, SHOPPING, ETC

PlaceCategory (VO)            // BR-4.3
  - CategoryType type
  - int rawContentTypeId      // 원본 보존 (폴백 포함)
```

## 4. 지역 (enum + VO)
```
enum Region { GUNSAN, JEONJU }

LDongCode (VO)
  - String lDongRegnCd     // 전북=52
  - String lDongSignguCd   // 군산=130, 전주=110

RegionCatalog (상수 정의, F5)
  - GUNSAN → LDongCode("52","130")
  - JEONJU → LDongCode("52","110")
```

## 5. 설정 바인딩 (VO)
```
TourApiProperties (@ConfigurationProperties "tourapi")
  - String baseUrl
  - String serviceKey      // 환경변수/시크릿 주입 (문서/코드에 값 노출 금지)
  - Duration connectTimeout, readTimeout
  - Duration cacheTtl      // 30분+ (BR-6)
```

> **영속(엔티티) 없음**: U1-a는 DB 테이블을 만들지 않음. User/Course/Run/Badge 엔티티는 각 도메인 유닛(U2~U5)에서 정의.
