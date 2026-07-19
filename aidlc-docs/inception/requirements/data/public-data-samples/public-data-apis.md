# 공공데이터 API 샘플 정리

> **용도**: 각 공공데이터 API의 **URL / 요청 파라미터 / 응답 샘플**을 여기에 복붙해 주세요.
> 응답은 **1~2건만** 넣어도 필드 파악에 충분합니다. (많이 넣을 필요 없음)
> 아래 블록을 복사해서 API 개수만큼 채워주세요. 모르는 칸은 비워두거나 `?`로 두면 됩니다.

## 채우는 법
- **엔드포인트 URL**: 실제 호출 주소 (예: `https://apis.data.go.kr/.../getList`)
- **인증키(serviceKey)**: 값은 넣지 말고 `발급받음 O / 아직 X`만 표기
- **파라미터**: 이름 · 필수여부 · 예시값 · 설명
- **응답 샘플**: JSON이면 JSON, XML이면 XML 그대로 복붙 (1건이면 충분)
- **쓸 필드 메모**: 우리가 실제로 쓸 것 같은 필드에 표시 (나중에 같이 확정)

---

## API 1. (지역기반 관광정보 조회 API)

- **출처/제공기관**: 공공데이터 API
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/areaBasedList2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) - _type 에서 JSON으로 설정
- **지역 필터 가능 여부**: (예: 시군구코드 / 좌표반경 / 없음) 법정동 시도 코드, 법정동 시군구 코드 로 시군구를 나눌 수 있고, 관광 타입과 분류 체계로 나눌 수 있음

### 요청 파라미터
| 파라미터명         | 필수 | 예시값      | 설명                                                                         |
|---------------|----|----------|----------------------------------------------------------------------------|
| serviceKey    | Y  | (키)      | 인증키                                                                        |
| MobileOS      | Y  | ETC      | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타)                                                                     |
| MobileApp     | Y  | APP      | 앱 이름                                                                       |
| numOfRows     | N  | 100      | 한페이지 결과 수                                                                  |
| pageNo        | N  | 1        | 페이지 번호                                                                     |
| _type         | N  | json     | 응답메세지 형식                                                                   |
| arrange       | N  | A        | 정렬구분(A=제목순, C=수정일순, D=생성일순) 대표이미지가 반드시 있는 정렬(O=제목순, Q=수정일순, R=생성일순)        |
| contentTypeId | N  | 12       | 관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID |
| lDongRegnCd   | N  | 52       | 법정동 시도 코드(법정동 시도코드 조회 참고)                                                  |
| lDongSignguCd | N  | 130      | 법정동 시군구 코드(법정동 시군구 코드 조회 참고)                                               |
| lclsSystm1    | N  | AC       | 분류체계 1Deth(분류체계코드 조회 참고)                                                   |
| lclsSystm2    | N  | AC01     | 분류체계 2Deth(분류체계코드 조회 참고)                                                   |
| lclsSystm3    | N  | AC010100 | 분류체계 3Deth(분류체계코드 조회 참고)                                                   |

### 요청 예시 (URL 또는 curl) <-- 포스트맨 curl로 붙여넣은거야
```
postman request 'https://apis.data.go.kr/B551011/KorService2/areaBasedList2?serviceKey=key&MobileOS=ETC&MobileApp=TestApp&contentTypeId=12&numOfRows=5&pageNo=1&_type=json&lDongRegnCd=52&lDongSignguCd=130' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
    "response": {
        "header": {
            "resultCode": "0000",
            "resultMsg": "OK"
        },
        "body": {
            "items": {
                "item": [
                    {
                        "addr1": "전북특별자치도 군산시 경촌4길 14",
                        "addr2": "(경암동)",
                        "areacode": "37",
                        "cat1": "A02",
                        "cat2": "A0203",
                        "cat3": "A02030100",
                        "contentid": "2605528",
                        "contenttypeid": "12",
                        "createdtime": "20190610190116",
                        "firstimage": "http://tong.visitkorea.or.kr/cms/resource/50/3080250_image2_1.JPG",
                        "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/50/3080250_image3_1.JPG",
                        "cpyrhtDivCd": "Type3",
                        "mapx": "126.7362330582",
                        "mapy": "35.9813520474",
                        "mlevel": "6",
                        "modifiedtime": "20250711100057",
                        "sigungucode": "2",
                        "tel": "",
                        "title": "경암동 철길마을",
                        "zipcode": "54033",
                        "lDongRegnCd": "52",
                        "lDongSignguCd": "130",
                        "lclsSystm1": "VE",
                        "lclsSystm2": "VE04",
                        "lclsSystm3": "VE040200"
                    },
                    {
                        "addr1": "전북특별자치도 군산시 옥도면 대장도리",
                        "addr2": "",
                        "areacode": "",
                        "cat1": "",
                        "cat2": "",
                        "cat3": "",
                        "contentid": "4065077",
                        "contenttypeid": "12",
                        "createdtime": "20260518092434",
                        "firstimage": "https://tong.visitkorea.or.kr/cms/resource/89/3588689_image2_1.jpg",
                        "firstimage2": "https://tong.visitkorea.or.kr/cms/resource/89/3588689_image3_1.jpg",
                        "cpyrhtDivCd": "Type1",
                        "mapx": "126.4712000000",
                        "mapy": "35.8168000000",
                        "mlevel": "",
                        "modifiedtime": "20260618165204",
                        "sigungucode": "",
                        "tel": "",
                        "title": "고군산군도",
                        "zipcode": "54000",
                        "lDongRegnCd": "52",
                        "lDongSignguCd": "130",
                        "lclsSystm1": "NA",
                        "lclsSystm2": "NA02",
                        "lclsSystm3": "NA020500"
                    },
                    {
                        "addr1": "전북특별자치도 군산시 옥도면",
                        "addr2": "",
                        "areacode": "37",
                        "cat1": "A02",
                        "cat2": "A0205",
                        "cat3": "A02050100",
                        "contentid": "2703743",
                        "contenttypeid": "12",
                        "createdtime": "20210114002700",
                        "firstimage": "http://tong.visitkorea.or.kr/cms/resource/36/3426836_image2_1.jpg",
                        "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/36/3426836_image3_1.jpg",
                        "cpyrhtDivCd": "Type1",
                        "mapx": "126.4443606944",
                        "mapy": "35.8130885149",
                        "mlevel": "6",
                        "modifiedtime": "20250611175426",
                        "sigungucode": "2",
                        "tel": "",
                        "title": "고군산대교",
                        "zipcode": "54000",
                        "lDongRegnCd": "52",
                        "lDongSignguCd": "130",
                        "lclsSystm1": "VE",
                        "lclsSystm2": "VE01",
                        "lclsSystm3": "VE010300"
                    }
                ]
            },
            "numOfRows": 3,
            "pageNo": 1,
            "totalCount": 92
        }
    }
}
```

### 우리가 쓸 필드 메모
-
지역기반 관광정보 조회 - 그 지역에 어떤 것들이 있는지 확인할 수 있음
---

## API 2. (위치기반 관광정보 조회 API)

- **출처/제공기관**: 공공데이터 포털 
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/locationBasedList2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) - _type으로 JSON 설정
- **지역 필터 가능 여부**: 

### 요청 파라미터
| 파라미터명         | 필수 | 예시값      | 설명                                                                                |
|---------------|---|----------|-----------------------------------------------------------------------------------|
| serviceKey    | Y | (키)      | 인증키                                                                               |
| MobileOS      | Y | ETC      | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타)                                                                            |
| MobileApp     | Y | APP      | 앱 이름                                                                              |
| mapX          | Y | 126.9817 | GPS X좌표(WGS84 경도좌표)                                                               |
| mapY          | Y | 37.6590  | GPS Y좌표(WGS84 위도좌표)                                                               |
| radius        | Y | 5000     | 거리반경(단위: m), Max값 20000m=20km                                                     |
| numOfRows     | N | 100      | 한페이지 결과 수                                                                         |
| pageNo        | N | 1        | 페이지 번호                                                                            |
| _type         | N | json     | 응답메세지 형식                                                                          |
| arrange       | N | A        | 정렬구분(A=제목순, C=수정일순, D=생성일순, E=거리순) 대표이미지가 반드시 있는 정렬(O=제목순, Q=수정일순, R=생성일순, S=거리순) |
| contentTypeId | N | 12       | 관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID        |
| lDongRegnCd   | N | 52       | 법정동 시도 코드(법정동 시도코드 조회 참고)                                                         |
| lDongSignguCd | N | 130      | 법정동 시군구 코드(법정동 시군구 코드 조회 참고)                                                      |
| lclsSystm1    | N | AC       | 분류체계 1Deth(분류체계코드 조회 참고)                                                          |
| lclsSystm2    | N | AC01     | 분류체계 2Deth(분류체계코드 조회 참고)                                                          |
| lclsSystm3    | N | AC010100 | 분류체계 3Deth(분류체계코드 조회 참고)                                                          |


### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/locationBasedList2?serviceKey=key&MobileOS=ETC&MobileApp=TestApp&mapX=126.9817&mapY=37.6590&radius=5000&contentTypeId=39&numOfRows=3&pageNo=1&arrange=E&_type=json' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
    "response": {
        "header": {
            "resultCode": "0000",
            "resultMsg": "OK"
        },
        "body": {
            "items": {
                "item": [
                    {
                        "addr1": "서울특별시 강북구 삼양로181길 141-5 (우이동)",
                        "addr2": "",
                        "zipcode": "01000",
                        "areacode": "1",
                        "cat1": "A05",
                        "cat2": "A0502",
                        "cat3": "A05020100",
                        "contentid": "2840195",
                        "contenttypeid": "39",
                        "createdtime": "20220819172911",
                        "dist": "2580.2428327635644",
                        "firstimage": "http://tong.visitkorea.or.kr/cms/resource/94/2840194_image2_1.jpg",
                        "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/94/2840194_image3_1.jpg",
                        "cpyrhtDivCd": "Type3",
                        "mapx": "127.0076182199",
                        "mapy": "37.6694622455",
                        "mlevel": "6",
                        "modifiedtime": "20250924091256",
                        "sigungucode": "3",
                        "tel": "",
                        "title": "왕의장어",
                        "lDongRegnCd": "11",
                        "lDongSignguCd": "305",
                        "lclsSystm1": "FD",
                        "lclsSystm2": "FD01",
                        "lclsSystm3": "FD010100"
                    },
                    {
                        "addr1": "서울특별시 강북구 4.19로 107 (수유동)",
                        "addr2": "",
                        "zipcode": "01017",
                        "areacode": "1",
                        "cat1": "A05",
                        "cat2": "A0502",
                        "cat3": "A05020900",
                        "contentid": "2840190",
                        "contenttypeid": "39",
                        "createdtime": "20220819172335",
                        "dist": "2603.5111781543606",
                        "firstimage": "http://tong.visitkorea.or.kr/cms/resource/85/2840185_image2_1.jpg",
                        "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/85/2840185_image3_1.jpg",
                        "cpyrhtDivCd": "Type3",
                        "mapx": "127.0038026375",
                        "mapy": "37.6436124062",
                        "mlevel": "6",
                        "modifiedtime": "20250924091027",
                        "sigungucode": "3",
                        "tel": "",
                        "title": "미즐카페엠",
                        "lDongRegnCd": "11",
                        "lDongSignguCd": "305",
                        "lclsSystm1": "FD",
                        "lclsSystm2": "FD05",
                        "lclsSystm3": "FD050100"
                    },
                    {
                        "addr1": "서울특별시 강북구 삼양로181길 142 옥류정",
                        "addr2": "",
                        "zipcode": "01000",
                        "areacode": "1",
                        "cat1": "A05",
                        "cat2": "A0502",
                        "cat3": "A05020900",
                        "contentid": "2838291",
                        "contenttypeid": "39",
                        "createdtime": "20220817164025",
                        "dist": "2611.2077506869605",
                        "firstimage": "http://tong.visitkorea.or.kr/cms/resource/85/2838285_image2_1.jpg",
                        "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/85/2838285_image3_1.jpg",
                        "cpyrhtDivCd": "Type3",
                        "mapx": "127.0079348542",
                        "mapy": "37.6695789043",
                        "mlevel": "6",
                        "modifiedtime": "20250904155327",
                        "sigungucode": "3",
                        "tel": "",
                        "title": "옥류헌릴렉스",
                        "lDongRegnCd": "11",
                        "lDongSignguCd": "305",
                        "lclsSystm1": "FD",
                        "lclsSystm2": "FD05",
                        "lclsSystm3": "FD050100"
                    }
                ]
            },
            "numOfRows": 3,
            "pageNo": 1,
            "totalCount": 27
        }
    }
}
```

### 우리가 쓸 필드 메모
-
좌표 값을 가지고 위치 기반에서 관광 정보를 볼 때 사용할 API
---

## API 3. (법정동 코드 조회API)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/ldongCode2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**: 

### 요청 파라미터
| 파라미터명       | 필수 | 예시값  | 설명                                                             |
|-------------|---|------|----------------------------------------------------------------|
| serviceKey  | Y | (키)  | 인증키                                                            |
| MobileOS    | Y | ETC  | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타)                                                        |
| MobileApp   | Y | APP  | 앱 이름                                                           |
| numOfRows   | N | 100  | 한페이지 결과 수                                                      |
| pageNo      | N | 1    | 페이지 번호                                                         |
| _type       | N | json | 응답메세지 형식                                                       |
| lDongRegnCd | N | 52   | 법정동 시도 코드(lDongRegnCd 해당되는 법정동 시군구 코드 조회, 입력이 없을 시 전체 시도목록 호출) |
| lDongListYn | N | Y    | 법정동 목록조회 예부(N:코드조회, Y:전체목록조회)                                  |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/ldongCode2?serviceKey=key&MobileOS=WEB&MobileApp=Test&_type=json&numOfRows=3&lDongRegnCd=52' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
    "response": {
        "header": {
            "resultCode": "0000",
            "resultMsg": "OK"
        },
        "body": {
            "items": {
                "item": [
                    {
                        "rnum": 1,
                        "code": "110",
                        "name": "전주시"
                    },
                    {
                        "rnum": 2,
                        "code": "111",
                        "name": "전주시 완산구"
                    },
                    {
                        "rnum": 3,
                        "code": "113",
                        "name": "전주시 덕진구"
                    }
                ]
            },
            "numOfRows": 3,
            "pageNo": 1,
            "totalCount": 16
        }
    }
}
```

### 우리가 쓸 필드 메모
-
우리는 전북 중에서 우선은 군산, 전주 이것들을 쓸 가능성이 높아
---

## API 4. (분류체계 코드 조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/lclsSystmCode2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명      | 필수 | 예시값      | 설명                                               |
|------------|---|----------|--------------------------------------------------|
| serviceKey | Y | (키)      | 인증키                                              |
| MobileOS   | Y | ETC      | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp  | Y | APP      | 앱 이름                                             |
| numOfRows  | N | 100      | 한페이지 결과 수                                        |
| pageNo     | N | 1        | 페이지 번호                                           |
| _type      | N | json     | 응답메세지 형식                                         |
| lclsSystm1 | N | FD       | 분류체계 1Depth 코드                                   |
| lclsSystm2 | N | FD02     | 분류체계 2Depth 코드(lclsSystm1 필수)                    |
| lclsSystm3 | N | FD020100 | 분류체계 3Depth 코드(lclsSystm1, lclsSystm2 필수)        |
| lclsSystm1 | N | N        | 분류체계 목록조회 여부(N:코드조회, Y:전체목록조회)                   |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/lclsSystmCode2?serviceKey=key&MobileOS=WEB&MobileApp=test&_type=json&lclsSystm1=FD&lclsSystm2=FD02&lclsSystm3=FD020100' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "code": "FD020100",
            "name": "중식",
            "rnum": 1
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

### 우리가 쓸 필드 메모
-
분류체계코드목록을 1Deth, 2Deth, 3Deth 코드별 조회하는 기능
---

## API 5. (공통정보 조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/detailCommon2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명      | 필수 | 예시값     | 설명                                               |
|------------|---|---------|--------------------------------------------------|
| serviceKey | Y | (키)     | 인증키                                              |
| MobileOS   | Y | ETC     | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp  | Y | APP     | 앱 이름                                             |
| contentId  | Y | 2605528 | 콘텐츠ID                                            |
| numOfRows  | N | 100     | 한페이지 결과 수                                        |
| pageNo     | N | 1       | 페이지 번호                                           |
| _type      | N | json    | 응답메세지 형식                                         |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/detailCommon2?MobileOS=ETC&MobileApp=testAPP&_type=json&contentId=2605528&numOfRows=3&pageNo=1&serviceKey=key' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "2605528",
            "contenttypeid": "12",
            "title": "경암동 철길마을",
            "createdtime": "20190610190116",
            "modifiedtime": "20250711100057",
            "tel": "",
            "telname": "",
            "homepage": "<a href=\"https://www.gunsan.go.kr/tour/m2099/view/433883?\" target=\"_blank\" title=\"새창: 군산 문화관광 홈페이지로 이동\">https://www.gunsan.go.kr/tour</a>",
            "firstimage": "http://tong.visitkorea.or.kr/cms/resource/50/3080250_image2_1.JPG",
            "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/50/3080250_image3_1.JPG",
            "cpyrhtDivCd": "Type3",
            "areacode": "37",
            "sigungucode": "2",
            "lDongRegnCd": "52",
            "lDongSignguCd": "130",
            "lclsSystm1": "VE",
            "lclsSystm2": "VE04",
            "lclsSystm3": "VE040200",
            "cat1": "A02",
            "cat2": "A0203",
            "cat3": "A02030100",
            "addr1": "전북특별자치도 군산시 경촌4길 14",
            "addr2": "(경암동)",
            "zipcode": "54033",
            "mapx": "126.7362330582",
            "mapy": "35.9813520474",
            "mlevel": "6",
            "overview": "경암동 철길마을은 1944년 전라북도 군산시 경암동에 준공하여 페이퍼 코리아 공장과 군산역을 연결하는 총연장 2.5km 철로 주변의 마을을 총괄하여 붙인 이름이다. 명칭의 유래는 마을이 위치한 행정 구역 명칭에 따라 철로 주변에 형성된 마을을 경암동 철길 마을이라 불렀다. 1944년 일제 강점기 개설된 철도 주변에 사람들이 모여 살기 시작하면서 동네를 이루었고, 1970년대 들어 본격적으로 마을이 형성되었다. 현재는 기차는 운행하지 않지만, 철길이 그대로 남아 근대 추억을 자극하는 군산의 관광명소이다. \n경암동 철길은 일제 강점기인 1944년에 신문 용지 재료를 실어 나르기 위해 최초로 개설되었다. 1950년대 중반까지는 ‘북선 제지 철도’라고 불렀으며, 1970년대 초까지는 ‘고려 제지 철도’, 그 이후에는 ‘세대 제지선’ 혹은 ‘세풍 철도’라고 불리다가 세풍 그룹이 부도나면서 새로 인수한 업체 이름을 따서 현재는 ‘페이퍼 코리아선’으로 불리고 있다.\n경암동 철길마을은 1970~80년대의 풍경을 재현하여 레트로 감성을 느낄 수 있는 곳으로 유명하다. 곳곳에는 오래된 주택과 가게들이 남아 있어, 마치 과거로 시간 여행을 온 듯한 느낌을 받을 수 있다. 또한, 철길 양옆으로 뽑기, 달고나, 딱지 등을 팔고 있고, 예전의 교복을 입고 사진 찍기 등 먹거리, 즐길거리가 있어 여행객들에게 인기가 많다."
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

### 우리가 쓸 필드 메모
-
타입별공통 정보기본정보,약도이미지,대표이미지,분류정보,지역정보,주소정보,좌표정보,개요정보,길안내정보,이미지정보,연계관광정보목록을 조회하는 기능
---

## API 6. (소개정보조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/detailIntro2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명         | 필수 | 예시값     | 설명                                               |
|---------------|----|---------|--------------------------------------------------|
| serviceKey    | Y  | (키)     | 인증키                                              |
| MobileOS      | Y  | ETC     | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp     | Y  | APP     | 앱 이름                                             |
| contentId     | Y  | 2605528 | 콘텐츠ID                                            |
| contentTypeId | Y  | 12      | 관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID |
| numOfRows     | N  | 100     | 한페이지 결과 수                                        |
| pageNo        | N  | 1       | 페이지 번호                                           |
| _type         | N  | json    | 응답메세지 형식                                         |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/detailIntro2?MobileOS=ETC&MobileApp=TestAPP&_type=json&contentId=2605528&contentTypeId=12&numOfRows=3&pageNo=1&serviceKey=key' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "2605528",
            "contenttypeid": "12",
            "heritage1": "0",
            "heritage2": "0",
            "heritage3": "0",
            "infocenter": "063-454-3349",
            "opendate": "",
            "restdate": "연중무휴",
            "expguide": "교복 입기 체험",
            "expagerange": "",
            "accomcount": "",
            "useseason": "",
            "usetime": "상시 개방",
            "parking": "가능<br>요금 (무료)",
            "chkbabycarriage": "",
            "chkpet": "",
            "chkcreditcard": ""
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

### 우리가 쓸 필드 메모
-
상세소개 쉬는날, 개장기간 등 내역을 조회하는 기능
---

## API 7. (반복정보조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/detailInfo2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명      | 필수 | 예시값      | 설명                                               |
|------------|---|----------|--------------------------------------------------|
| serviceKey    | Y  | (키)     | 인증키                                              |
| MobileOS      | Y  | ETC     | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp     | Y  | APP     | 앱 이름                                             |
| contentId     | Y  | 2605528 | 콘텐츠ID                                            |
| contentTypeId | Y  | 12      | 관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID |
| numOfRows     | N  | 100     | 한페이지 결과 수                                        |
| pageNo        | N  | 1       | 페이지 번호                                           |
| _type         | N  | json    | 응답메세지 형식                                         |




### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/detailInfo2?serviceKey=key&MobileOS=ETC&MobileApp=testApp&_type=json&contentId=2605528&contentTypeId=12&numOfRows=3&pageNo=1' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "2605528",
            "contenttypeid": "12",
            "serialnum": "0",
            "infoname": "촬영장소",
            "infotext": "영화 ‘남자가 사랑할 때’ 촬영지",
            "fldgubun": "2"
          },
          {
            "contentid": "2605528",
            "contenttypeid": "12",
            "serialnum": "1",
            "infoname": "입 장 료",
            "infotext": "무료",
            "fldgubun": "3"
          }
        ]
      },
      "numOfRows": 2,
      "pageNo": 1,
      "totalCount": 2
    }
  }
}
```

### 우리가 쓸 필드 메모
-
추가 관광정보 상세내역을 조회한다. 상세반복정보를 안내URL의 국문관광정보 상세 매뉴얼 문서를 참고하시기 바랍니다.
---

<!--
블록이 더 필요하면 위 "## API N." 블록을 그대로 복사해서 이어 붙이세요.
후보 데이터(참고): 공중화장실 / 가로등 / 약국 / 모범음식점 / 착한가격업소 등
파일(csv/xlsx) 형태 원본은 ../file-data/ 폴더에 올려주세요.
-->

### 우선은 이렇게 쓸건데 api response 같은것들을 다 가져오긴 너무 많아서 
### https://www.data.go.kr/data/15101578/openapi.do#/
### 이렇게 링크 남겨놨어 여기에서 추가적으로 더 확인할 수 있어

## API 8. (키워드 검색 조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/searchKeyword2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명         | 필수 | 예시값  | 설명                                               |
|---------------|----|------|--------------------------------------------------|
| serviceKey    | Y  | (키)  | 인증키                                              |
| MobileOS      | Y  | ETC  | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp     | Y  | APP  | 앱 이름                                             |
| keyword       | Y  | 카페   | 검색 요청 키워드: 국문 = 인코딩 필요                           |
| numOfRows     | N  | 100  | 한페이지 결과 수                                        |
| pageNo        | N  | 1    | 페이지 번호                                           |
| _type         | N  | json | 응답메세지 형식                                         |
| lDongRegnCd   | N  | 52   | 법정동 시도코드                                         | 
| lDongSignguCd | N  | 130  | 법정동 시군구 코드                                       |
| lclsSystm1 | N | FD       | 분류체계 1Depth 코드                                   |
| lclsSystm2 | N | FD02     | 분류체계 2Depth 코드(lclsSystm1 필수)                    |
| lclsSystm3 | N | FD020100 | 분류체계 3Depth 코드(lclsSystm1, lclsSystm2 필수)        |




### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/searchKeyword2?serviceKey=key&MobileOS=ETC&MobileApp=testApp&numOfRows=3&pageNo=1&_type=json&arrange=A&keyword=%EC%B9%B4%ED%8E%98&lDongRegnCd=52&lDongSignguCd=130' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "addr1": "전북특별자치도 군산시 성산면 오성로 134-17",
            "addr2": "",
            "zipcode": "54045",
            "areacode": "37",
            "cat1": "A05",
            "cat2": "A0502",
            "cat3": "A05020900",
            "contentid": "2783366",
            "contenttypeid": "39",
            "createdtime": "20211130185732",
            "firstimage": "http://tong.visitkorea.or.kr/cms/resource/96/2783596_image2_1.jpg",
            "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/96/2783596_image3_1.jpg",
            "cpyrhtDivCd": "Type3",
            "mapx": "126.7773094151",
            "mapy": "36.0016143272",
            "mlevel": "6",
            "modifiedtime": "20251112090124",
            "sigungucode": "2",
            "tel": "",
            "title": "당골 한옥카페",
            "lDongRegnCd": "52",
            "lDongSignguCd": "130",
            "lclsSystm1": "FD",
            "lclsSystm2": "FD05",
            "lclsSystm3": "FD050100"
          },
          {
            "addr1": "전북특별자치도 군산시 해망로 118",
            "addr2": "2동 1층",
            "zipcode": "54026",
            "areacode": "37",
            "cat1": "A05",
            "cat2": "A0502",
            "cat3": "A05020900",
            "contentid": "2877157",
            "contenttypeid": "39",
            "createdtime": "20221027113303",
            "firstimage": "http://tong.visitkorea.or.kr/cms/resource/52/2877152_image2_1.jpg",
            "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/52/2877152_image3_1.jpg",
            "cpyrhtDivCd": "Type3",
            "mapx": "126.7241840099",
            "mapy": "35.9862235076",
            "mlevel": "6",
            "modifiedtime": "20250116134800",
            "sigungucode": "2",
            "tel": "",
            "title": "리투스카페",
            "lDongRegnCd": "52",
            "lDongSignguCd": "130",
            "lclsSystm1": "FD",
            "lclsSystm2": "FD05",
            "lclsSystm3": "FD050100"
          },
          {
            "addr1": "전북특별자치도 군산시 장자도2길 31",
            "addr2": "",
            "zipcode": "54000",
            "areacode": "37",
            "cat1": "A05",
            "cat2": "A0502",
            "cat3": "A05020900",
            "contentid": "2877189",
            "contenttypeid": "39",
            "createdtime": "20221027120251",
            "firstimage": "http://tong.visitkorea.or.kr/cms/resource/86/2877186_image2_1.jpg",
            "firstimage2": "http://tong.visitkorea.or.kr/cms/resource/86/2877186_image3_1.jpg",
            "cpyrhtDivCd": "Type3",
            "mapx": "126.3971093695",
            "mapy": "35.8129182954",
            "mlevel": "6",
            "modifiedtime": "20250113185756",
            "sigungucode": "2",
            "tel": "",
            "title": "카페라파르",
            "lDongRegnCd": "52",
            "lDongSignguCd": "130",
            "lclsSystm1": "FD",
            "lclsSystm2": "FD05",
            "lclsSystm3": "FD050100"
          }
        ]
      },
      "numOfRows": 3,
      "pageNo": 1,
      "totalCount": 6
    }
  }
}
```

### 우리가 쓸 필드 메모
-
키워드 검색용
---

## API 6. (소개정보조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/detailIntro2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명         | 필수 | 예시값     | 설명                                               |
|---------------|----|---------|--------------------------------------------------|
| serviceKey    | Y  | (키)     | 인증키                                              |
| MobileOS      | Y  | ETC     | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp     | Y  | APP     | 앱 이름                                             |
| contentId     | Y  | 2605528 | 콘텐츠ID                                            |
| contentTypeId | Y  | 12      | 관광타입(12:관광지, 14:문화시설, 15:축제공연행사, 25:여행코스, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) ID |
| numOfRows     | N  | 100     | 한페이지 결과 수                                        |
| pageNo        | N  | 1       | 페이지 번호                                           |
| _type         | N  | json    | 응답메세지 형식                                         |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/detailIntro2?MobileOS=ETC&MobileApp=TestAPP&_type=json&contentId=2605528&contentTypeId=12&numOfRows=3&pageNo=1&serviceKey=key' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "2783366",
            "contenttypeid": "39",
            "seat": "",
            "kidsfacility": "0",
            "firstmenu": "흑임자크림라떼",
            "treatmenu": "쑥크림라떼 / 쑥덕바삭이 / 당골크림라떼 등",
            "smoking": "",
            "packing": "",
            "infocenterfood": "0507-1332-7605",
            "scalefood": "",
            "parkingfood": "가능",
            "opendatefood": "",
            "opentimefood": "11:30~19:00",
            "restdatefood": "매주 화요일",
            "discountinfofood": "",
            "chkcreditcardfood": "",
            "reservationfood": "",
            "lcnsno": "20200488014"
          }
        ]
      },
      "numOfRows": 1,
      "pageNo": 1,
      "totalCount": 1
    }
  }
}
```

### 우리가 쓸 필드 메모
-
음식점 필드
---

## API 9. (이미지 정보 조회)

- **출처/제공기관**: 공공데이터 포털
- **엔드포인트 URL**: https://apis.data.go.kr/B551011/KorService2/detailImage2
- **HTTP Method**: GET
- **인증키 필요**: (O / X) — O
- **응답 형식**: (JSON / XML) Json
- **지역 필터 가능 여부**:

### 요청 파라미터
| 파라미터명      | 필수 | 예시값     | 설명                                              |
|------------|----|---------|-------------------------------------------------|
| serviceKey | Y  | (키)     | 인증키                                             |
| MobileOS   | Y  | ETC     | OS 구분 : IOS (아이폰), AND (안드로이드), WEB (웹), ETC(기타) |
| MobileApp  | Y  | APP     | 앱 이름                                            |
| contentId  | Y  | 2605528 | 콘텐츠ID                                           |
| imageYN    | N  | Y       | 이미지조회1: Y=콘텐츠 이미지 조회 N=음식점타입의음식메뉴이미지            |
| numOfRows  | N  | 100     | 한페이지 결과 수                                       |
| pageNo     | N  | 1       | 페이지 번호                                          |
| _type      | N  | json    | 응답메세지 형식                                        |



### 요청 예시
```
postman request 'https://apis.data.go.kr/B551011/KorService2/detailImage2?serviceKey=key&MobileOS=ETC&MobileApp=testAPp&_type=json&contentId=2783366&imageYN=Y&numOfRows=3&pageNo=1' \
  --header 'Cookie: NCPVPCLB={{COOKIE}}'
```

### 응답 샘플 (1건)
```json
{
  "response": {
    "header": {
      "resultCode": "0000",
      "resultMsg": "OK"
    },
    "body": {
      "items": {
        "item": [
          {
            "contentid": "2783366",
            "originimgurl": "http://tong.visitkorea.or.kr/cms/resource/92/2783592_image2_1.jpg",
            "imgname": "01 전북_군산_당골카페_외관_추가제공 리사이징 1",
            "smallimageurl": "http://tong.visitkorea.or.kr/cms/resource/92/2783592_image3_1.jpg",
            "cpyrhtDivCd": "Type3",
            "serialnum": "2783592_1"
          },
          {
            "contentid": "2783366",
            "originimgurl": "http://tong.visitkorea.or.kr/cms/resource/93/2783593_image2_1.jpg",
            "imgname": "02 전북_군산_당골카페_외관_추가제공 리사이징 3",
            "smallimageurl": "http://tong.visitkorea.or.kr/cms/resource/93/2783593_image3_1.jpg",
            "cpyrhtDivCd": "Type3",
            "serialnum": "2783593_3"
          },
          {
            "contentid": "2783366",
            "originimgurl": "http://tong.visitkorea.or.kr/cms/resource/94/2783594_image2_1.jpg",
            "imgname": "03 전북_군산_당골카페_외관_추가제공 리사이징 7",
            "smallimageurl": "http://tong.visitkorea.or.kr/cms/resource/94/2783594_image2_1.jpg",
            "cpyrhtDivCd": "Type3",
            "serialnum": "2783594_2"
          }
        ]
      },
      "numOfRows": 3,
      "pageNo": 1,
      "totalCount": 4
    }
  }
}
```

### 우리가 쓸 필드 메모
-
사진 
---

