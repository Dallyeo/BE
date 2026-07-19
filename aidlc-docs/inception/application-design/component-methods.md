# Component Methods — Dallyeo

> 메서드 시그니처(고수준). 반환 타입은 개념적 DTO — 상세 필드/검증/비즈니스 규칙은 Functional Design(단위별)에서 확정.
> 컨트롤러 응답은 공통 래퍼 `ApiResponse<T>`로 감싼다(생략 표기).

---

## Unit 1. 공통·기반

### TourApiClient (Facade, RestClient 기반)
```
List<TourItem> searchKeyword(KeywordQuery q)              // searchKeyword2
List<TourItem> areaBasedList(AreaQuery q)                 // areaBasedList2
List<TourItem> locationBasedList(LocationQuery q)         // locationBasedList2 (dist 포함)
TourCommon     detailCommon(String contentId)             // detailCommon2
TourIntro      detailIntro(String contentId, int typeId)  // detailIntro2 (타입별 분기)
List<TourInfo> detailInfo(String contentId, int typeId)   // detailInfo2
List<TourImage> detailImage(String contentId)             // detailImage2
```
> 각 메서드는 Resilience4j(타임아웃/재시도/서킷브레이커) 적용 + Redis 캐시 대상.

### JwtProvider
```
String   issueAccessToken(Long userId)
String   issueRefreshToken(Long userId)
boolean  validate(String token)
Long     getUserId(String token)
```

### RegionCodeMapper
```
LDongCode toLDongCode(Region region)   // 전북52 + 군산130/전주110
Region    fromLDongCode(LDongCode c)
```

---

## Unit 2. 지역·코스조회 🌐

### RegionController / RegionService
```
GET /regions
  List<RegionDto> RegionService.getRegions()
```

### CourseController / CourseQueryService
```
GET /courses?region&distance
  List<CourseSummaryDto> CourseQueryService.getCourses(Region region, CourseDistance distance)
GET /courses/{id}
  CourseDetailDto        CourseQueryService.getCourse(Long id)   // 없으면 404
```

---

## Unit 3. 장소·배지 🌐

### PlaceController / PlaceService
```
GET /places/search?keyword&region&category
  List<PlaceSummaryDto> PlaceService.search(KeywordQuery q)
GET /places?region&category
  List<PlaceSummaryDto> PlaceService.listByRegion(AreaQuery q)
GET /places/nearby?lat&lng&radius&category
  List<PlaceSummaryDto> PlaceService.nearby(LocationQuery q)     // dist 포함
GET /places/{id}?type
  PlaceDetailDto        PlaceService.getDetail(String contentId, int contentTypeId)
```

### PlaceDetailAssembler (병렬 조합)
```
PlaceDetailDto assemble(String contentId, int typeId)
  // detailCommon/Intro/Info/Image 를 CompletableFuture 병렬 호출 후 병합
```

### PlaceMapper
```
PlaceSummaryDto toSummary(TourItem item)     // 좌표 파싱, 빈값 null, category 매핑
PlaceDetailDto  toDetail(TourCommon c, TourIntro i, List<TourInfo> infos, List<TourImage> imgs)
PlaceCategory   resolveCategory(int typeId, String lclsSystm2)  // 39+FD05→CAFE 등
```

### BadgeService
```
List<Badge> findBadges(String name, String address)   // 정규화 매칭
Map<PlaceKey, List<Badge>> findBadges(Collection<PlaceRef> places)  // 목록 일괄
```

### BadgeCsvLoader / AddressNormalizer
```
void loadModelRestaurants(Path csv)   // UTF-8
void loadGoodPriceShops(Path csv)     // EUC-KR→UTF-8, 요식업만
String AddressNormalizer.normalize(String nameOrAddress)
```

---

## Unit 4. 인증·사용자 🔒

### AuthController / AuthService
```
POST /auth/login/{provider}
  TokenResponse AuthService.login(Provider provider, String authorization)  // 가입 or 조회 + 토큰
POST /auth/refresh
  TokenResponse AuthService.refresh(String refreshToken)                    // 무효/불일치 401
POST /auth/logout
  void          AuthService.logout(Long userId)                            // 204
```

### OAuthClient (Kakao/Apple)
```
OAuthUser verify(String authorizationOrIdentityToken)   // 소셜 검증 + 사용자정보
```

### UserController / UserService
```
GET /users/me
  UserProfileDto UserService.getMyProfile(Long userId)
PATCH /users/me
  UserProfileDto UserService.updateProfile(Long userId, UpdateProfileCommand cmd)  // 온보딩 신체정보 포함
DELETE /users/me
  void           UserService.deleteAccount(Long userId)   // 204
```

---

## Unit 5. 러닝기록·코스생성 🔒

### RunController / RunService
```
POST /runs
  RunDto      RunService.save(Long userId, CreateRunCommand cmd)   // 201
GET /runs?from&to
  List<RunSummaryDto> RunService.getMyRuns(Long userId, DateRange range)
GET /runs/{id}
  RunDetailDto RunService.getRun(Long userId, Long runId)          // 타인 403/404
```

### CourseCommandService 🔒
```
POST /courses
  CourseDetailDto CourseCommandService.create(Long userId, CreateCourseCommand cmd)  // Tmap JSON 저장, 201
```
