# TourAPI(KorService2) → Dallyeo API 필드 매핑

> **목적**: 한국관광공사 TourAPI 응답 필드를 우리 백엔드 장소(Places) API 스키마로 매핑.
> **소스**: `data/public-data-samples/public-data-apis.md` (실제 응답 샘플 7종)
> **원문**: https://www.data.go.kr/data/15101578/openapi.do (Swagger UI — 총 15개 오퍼레이션)
> **장소 데이터 정책**: TourAPI + 파일데이터(모범음식점/착한가격업소/공중화장실/약국 등) **병행**

---

## 0. 오퍼레이션 역할 요약

| 우리 기능 | TourAPI 오퍼레이션 | 비고 |
|---|---|---|
| 지역별 장소 목록 | `areaBasedList2` | 법정동 코드로 필터 |
| 주변 장소(반경) | `locationBasedList2` | 좌표+반경, `dist`(거리) 제공 |
| 키워드 검색 | `searchKeyword2` | ✅ 목록 API와 응답 필드 **동일** |
| 장소 상세(개요) | `detailCommon2` | overview/homepage 등 |
| 장소 상세(소개) | `detailIntro2` | **contentTypeId별 필드 다름** (관광지/음식점 확보) |
| 장소 상세(부가) | `detailInfo2` | infoname/infotext 반복 |
| 장소 이미지 | `detailImage2` | ✅ 여러 장. originimgurl/smallimageurl |
| 코드 참조 | `ldongCode2`, `lclsSystmCode2` | 지역/분류 코드표 |

---

## 1. 목록 아이템 매핑 (areaBasedList2 / locationBasedList2 → Place 목록)

우리 §6.1/6.3 목록 아이템 기준.

| 우리 필드 | TourAPI 필드 | 타입 | 변환/비고 |
|---|---|---|---|
| `id` | `contentid` | string | 장소 식별자(상세조회 키) |
| `name` | `title` | string | |
| `category` | `contenttypeid` (+`lclsSystm`) | enum | §3 카테고리 매핑 참조 |
| `latitude` | `mapy` | double | **문자열→double 파싱** (Y=위도) |
| `longitude` | `mapx` | double | (X=경도) 주의: X/Y ↔ lng/lat |
| `address` | `addr1` (+`addr2`) | string | `addr1 + " " + addr2` |
| `thumbnailUrl` | `firstimage2` | string\|null | 썸네일. 없으면 빈 문자열 → null 처리 |
| `distanceMeters` | `dist` | double | **locationBased 전용**(반경 조회 시만). 문자열→double |

**추가로 확보 가능한 필드**(필요 시): `tel`, `zipcode`, `areacode`, `sigungucode`, `lDongRegnCd`, `lDongSignguCd`, `cat1~3`, `lclsSystm1~3`, `firstimage`(원본), `modifiedtime`, `mlevel`.

**빈 값 주의**: 샘플에서 `areacode`, `cat1~3`, `firstimage`, `tel`, `sigungucode`가 `""`(빈 문자열)로 오는 케이스 존재 → 백엔드에서 `""`은 `null`로 정규화 권장.

---

## 2. 상세 매핑 (detailCommon2 + detailIntro2 + detailInfo2 → Place 상세)

우리 §6.2 장소 상세는 **3개 오퍼레이션 조합**으로 채운다.

### 2.1 detailCommon2 (공통·개요)
| 우리 필드 | TourAPI 필드 | 비고 |
|---|---|---|
| `id` | `contentid` | |
| `name` | `title` | |
| `description` | `overview` | HTML 태그/개행 포함 가능 → 정제 |
| `homepage` | `homepage` | **`<a href=...>` HTML** → href만 추출 |
| `imageUrl` | `firstimage` | 원본 이미지 |
| `address` | `addr1`+`addr2` | |
| `latitude/longitude` | `mapy`/`mapx` | |
| `tel` | `tel` | 빈 값 잦음 |

### 2.2 detailIntro2 (소개) — ⚠️ contentTypeId별 필드가 다름
샘플은 **관광지(12)** 기준:
| 우리 필드(후보) | TourAPI 필드(12 관광지) | 비고 |
|---|---|---|
| `businessHours` | `usetime` | 이용시간 |
| `restDate` | `restdate` | 쉬는 날 (예: "연중무휴") |
| `parking` | `parking` | 주차 (HTML `<br>` 포함) |
| `inquiry` | `infocenter` | 문의·안내 전화 |
| `petAllowed` | `chkpet` | |
| `creditCard` | `chkcreditcard` | |
| `strollerAllowed` | `chkbabycarriage` | |

**음식점(39)** — 필드명이 완전히 다름 (✅ 샘플 확보):
| 우리 필드(후보) | TourAPI 필드(39 음식점) | 비고 |
|---|---|---|
| `businessHours` | `opentimefood` | 예: "11:30~19:00" |
| `restDate` | `restdatefood` | 예: "매주 화요일" |
| `signatureMenu` | `firstmenu` | 대표메뉴 |
| `menu` | `treatmenu` | 취급메뉴 |
| `parking` | `parkingfood` | |
| `inquiry` | `infocenterfood` | 문의·안내 |
| `packagingAvailable` | `packing` | 포장 |
| `reservation` | `reservationfood` | 예약 |
| `creditCard` | `chkcreditcardfood` | |
| (참고) | `lcnsno` | 인허가번호(파일데이터 매칭 키 후보) |

> **매핑 분기 필수**: 상세 조회 시 `contentTypeId`로 관광지(12)/음식점(39) 필드셋 분기. 백엔드가 공통 응답 필드(`businessHours`/`restDate`/`parking`/`inquiry`)로 정규화해서 프론트에 통일 제공 권장.
> 💡 `lcnsno`(인허가번호)는 **모범음식점/착한가격업소 파일데이터와 매칭하는 키**로 활용 가능성 있음 (좌표/이름 매칭보다 정확).

### 2.3 detailInfo2 (반복 부가정보)
- 응답: `item[]` 각 `{ infoname, infotext, serialnum, fldgubun }`
- 우리 스키마: `extraInfo: [{ name: infoname, text: infotext }]` 형태로 배열 매핑
- 예: `촬영장소 / 영화 '남자가 사랑할 때' 촬영지`, `입장료 / 무료`

### 2.4 detailImage2 (이미지 목록) — 상세에서 사진 여러 장
- 파라미터: `contentId`(필수), `imageYN`(Y=콘텐츠 이미지 / N=음식점 메뉴 이미지)
- 응답: `item[]` 각 `{ originimgurl, smallimageurl, imgname, serialnum }`
| 우리 필드 | TourAPI 필드 | 비고 |
|---|---|---|
| `images[].url` | `originimgurl` | 원본 |
| `images[].thumbnailUrl` | `smallimageurl` | 썸네일 |
| `images[].name` | `imgname` | 이미지 설명 |

---

## 3. 카테고리(Category) 매핑

우리 `PlaceCategory` enum ↔ TourAPI 코드.

| 우리 enum | TourAPI 기준 | 비고 |
|---|---|---|
| `TOUR`(관광지) | `contenttypeid=12` | |
| `RESTAURANT`(음식점) | `contenttypeid=39` + `lclsSystm2 != FD05` | |
| `CAFE`(카페) | `contenttypeid=39` + `lclsSystm2=FD05` | ✅ 확정. 샘플: 당골한옥카페·리투스카페(cat3 A05020900 / FD05) |
| `CONVENIENCE`(편의시설) | ❌ TourAPI 없음 | **파일데이터**(화장실/약국 등)에서 공급 |

**기타 contentTypeId**: 14 문화시설, 15 축제공연행사, 25 여행코스, 28 레포츠, 32 숙박, 38 쇼핑 — 필요 시 enum 확장.

> **✅ 결정**: contentTypeId **전체 노출**(12/14/15/25/28/32/38/39). PlaceCategory enum도 전 타입 커버 필요. CAFE는 39+`lclsSystm2=FD05`로 세분 유지.

---

## 4. 지역(Region) 매핑

우리 `Region` enum ↔ 법정동 코드 (`ldongCode2` 응답 기준).

| 우리 enum | lDongRegnCd(시도) | lDongSignguCd(시군구) | 비고 |
|---|---|---|---|
| `GUNSAN`(군산) | `52`(전북) | `130` | 샘플 API1에서 사용 |
| `JEONJU`(전주) | `52`(전북) | `110` | ldongCode2에서 전주시=110 확인 |

> **주의**: TourAPI에는 **두 코드 체계**가 공존 — 구(舊) `areacode`/`sigungucode` 와 신(新) `lDongRegnCd`/`lDongSignguCd`. **필터 파라미터가 법정동(신) 체계**이므로 우리도 법정동 코드로 통일.
> **✅ 결정**: 전주는 **시 단위(110)로 통합**. `JEONJU` = lDongSignguCd 110 (완산/덕진 구 분리 안 함).

---

## 5. 아키텍처/설계 관점 (✅ 결정 반영)

1. **연동 방식**: ✅ **실시간 프록시**. 요청 시 TourAPI 직호출.
   - ⚠️ 리스크 관리 필요: 외부 장애/레이트리밋 대비 **타임아웃·재시도·서킷브레이커**, 응답 **단기 캐시(예: 수 분)** 권장. 배지(파일데이터)는 우리 DB에 있으므로 실시간 응답에 조인.
2. **상세 조회 N+1**: 상세 1건에 detailCommon/Intro/Info(+Image) 3~4콜 → **병렬 호출** 필요. 실시간이므로 응답 지연 주의.
3. **contentTypeId 보관**: `detailIntro2`가 typeId를 요구 → 목록에서 받은 `contenttypeid`를 프론트에 내려주고 상세 요청 시 함께 받거나, 백엔드가 detailCommon으로 typeId 확인 후 Intro 호출.
4. **파일데이터 병합 키**: ✅ **하이브리드 매칭**(업소명+주소 정규화 자동 후보 → 수동 확정). lcnsno/좌표 없음. **요식업만 적재**(비음식 15건 제외). 상세는 `data/file-data/badge-data-analysis.md` 참조.
5. **좌표 정밀도/파싱**: mapx/mapy가 문자열, 자리수 제각각(빈 값 포함) → 파싱·검증 규약.

---

## 6. 다음 액션
- [x] `searchKeyword2`(키워드 검색) 샘플 확보 — 목록 API와 응답 동일 확인
- [x] 음식점(39) `detailIntro2` 샘플 확보 — 필드셋 확정
- [x] `detailImage2`(이미지) 샘플 확보
- [ ] 위 ❓ 결정(카테고리 화이트리스트 / 전주 구 세분화 / 연동 방식 / 파일데이터 매칭 키) 확정
- [ ] 확정 후 `api-spec-draft.md` §6 Places를 TourAPI 실제 필드로 갱신 → v1.0
