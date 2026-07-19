# Business Logic Model — U1-a 공개 기반

> 기술 계약 중심의 기반 유닛. 도메인 비즈니스 로직보다 **공통 흐름(응답/예외/외부연동/정규화)** 을 정의.
> 확정 답변: F1=A, F2=A, F3=A(+엔드포인트별 좌표정책), F4=A(+원본 보존/WARN), F5=A, F6=C(TTL 30분+).

---

## 1. 공통 응답 흐름
- 모든 성공 응답은 `ApiResponse.success(data)` 로 감싼다.
- 데이터 없는 성공은 HTTP 204(본문 없음).
- 모든 실패는 `GlobalExceptionHandler`를 거쳐 `ApiResponse.failure(code, message, details?)` + 적절한 HTTP status.

```
Controller 반환값 T
  └─ (정상) → ApiResponse<T>{ success:true, data:T }         → 200
  └─ (본문없음) → 204
  └─ (예외) → GlobalExceptionHandler → ApiResponse{ success:false, error:{code,message,details?} } → 4xx/5xx
```

## 2. 예외 처리 흐름 (GlobalExceptionHandler)
| 예외 유형 | HTTP | error.code | details |
|---|---|---|---|
| Bean Validation 실패(`MethodArgumentNotValid`) | 400 | `VALIDATION_ERROR` | 필드 오류 목록 |
| 잘못된 요청(도메인 IllegalArgument) | 400 | `BAD_REQUEST` | — |
| 인증 실패 | 401 | `UNAUTHORIZED` | — |
| 권한 없음 | 403 | `FORBIDDEN` | — |
| 리소스 없음 | 404 | `NOT_FOUND` | — |
| 충돌 | 409 | `CONFLICT` | — |
| 외부 TourAPI 오류/서킷오픈/타임아웃 | 502 | `EXTERNAL_API_ERROR` | — |
| 미분류 서버 오류 | 500 | `INTERNAL_ERROR` | — |

> 도메인별 세부 코드(예: `COURSE_NOT_FOUND`)는 각 도메인 유닛에서 위 베이스 코드를 확장/특화.

## 3. TourAPI 연동 흐름 (TourApiClient)
```
서비스 → TourApiClient.op(params)
  1) 캐시 조회 (키 = op + 파라미터 해시)  ── HIT → 반환
  2) MISS → Resilience4j(타임아웃/재시도/서킷브레이커)로 외부 HTTP 호출
  3) 응답 파싱 → DTO 매핑 → 정규화(§4) 
  4) 캐시 저장 (TTL 30분+) → 반환
  실패(타임아웃/서킷오픈/HTTP 오류) → EXTERNAL_API_ERROR(502)
```
- 상세 조합(PlaceDetailAssembler)은 U3에서 이 클라이언트를 병렬 호출. U1-a는 **클라이언트+정규화+캐시/회복성 기반**만 제공.

## 4. 정규화 흐름 (TourAPI DTO → 내부 모델)
- **빈 문자열 `""` → `null`** (모든 필드 공통).
- **좌표/거리 문자열 → double 파싱**(`mapx`,`mapy`,`dist`).
- **좌표 파싱 실패 시 (엔드포인트별 정책, F3):**
  - `locationBasedList2`(반경) → 좌표 **필수** → 파싱 실패 항목 **제외**.
  - `areaBasedList2` / `searchKeyword2` → 좌표 **null 유지**, 항목은 **보존**(좌표 없어도 목록 가치).
- **카테고리 매핑(F4):** `contentTypeId`(+`lclsSystm2`) → `CategoryType`. 미매핑/애매 → **`ETC` 폴백** + **원본 `rawContentTypeId` 보존** + **WARN 로그**.

## 5. 지역 매핑 흐름 (RegionCodeMapper)
- `Region`(enum) ↔ 법정동코드. 정의는 **코드 상수(RegionCatalog)** (F5).
- `GUNSAN` → (lDongRegnCd=52, lDongSignguCd=130), `JEONJU` → (52, 110).
