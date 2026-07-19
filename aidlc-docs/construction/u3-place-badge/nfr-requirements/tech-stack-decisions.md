# Tech Stack Decisions — U3 장소·배지 🌐

> 기존 스택 재사용(U1-a TourApiClient/Resilience4j/Redis, U2 JPA/MySQL). U3 신규 결정만.

---

## 확정 기술 선택

| 관심사 | 선택 | 근거 |
|---|---|---|
| **외부 조회** | U1-a `TourApiClient` 확장(5 오퍼레이션) | search/list/nearby/detailCommon/detailIntro |
| **회복성/캐시** | Resilience4j 엔드포인트별 인스턴스 + `@Cacheable` | N1=A. `tourApiSearchKeyword`/`tourApiLocationBased`/`tourApiDetailCommon`/`tourApiDetailIntro` 추가(application.properties base-config=default) |
| **배지 영속** | JPA `Badge` 엔티티 + `(normalizedName, normalizedAddress)` 복합 인덱스 | N3=A |
| **CSV 파싱** | 단순 라인 파싱(구분자 콤마) 또는 경량 CSV 유틸 | 소량·고정 스키마 |
| **CSV 인코딩** | **파일별 명시** — 모범음식점 `UTF-8`, 착한가격업소 `EUC-KR` | ⚠️ 필수 |
| **정규화** | `AddressNormalizer`(공백/괄호/특수문자 제거) | 매칭 키 |
| **상세 조합** | `PlaceDetailAssembler` — common→intro 순차 | P3 |

## CSV 인코딩 주의 (N3 메모)
```
모범음식점_20260130.csv        → UTF-8
착한가격업소 현황_20260416.csv  → EUC-KR (MS949)
```
- `BadgeCsvLoader`는 파일별 Charset을 명시적으로 지정해 읽는다.
- **EUC-KR 파일을 UTF-8로 읽으면 업소명/주소가 깨져 정규화 매칭이 전부 실패**하므로 반드시 파일별 인코딩 지정.
- CSV는 `src/main/resources/data/`로 복사(classpath 번들).

## application.properties 추가 (Resilience4j 인스턴스)
```
resilience4j.circuitbreaker.instances.tourApiSearchKeyword.base-config=default
resilience4j.circuitbreaker.instances.tourApiLocationBased.base-config=default
resilience4j.circuitbreaker.instances.tourApiDetailCommon.base-config=default
resilience4j.circuitbreaker.instances.tourApiDetailIntro.base-config=default
resilience4j.retry.instances.<동일 4종>.base-config=default
```

## 유보 (범위 밖)
- detailInfo2/detailImage2(부가/갤러리) → 후속(P2=A).
- 배지 하이브리드 매칭·전주 CSV → 후속.
