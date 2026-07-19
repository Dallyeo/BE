# User Stories — Dallyeo

- **그룹핑**: 도메인 기반 (인증 / 사용자·프로필 / 지역 / 코스 / 러닝기록 / 장소 / 배지 / 공통·기반)
- **페르소나**: 러너 (P1)
- **세분화**: 중간 수준 (기능 단위)
- **인수 조건**: Given-When-Then (BDD)
- **우선순위**: MoSCoW (Must / Should / Could / Won't)
- **INVEST** 기준을 따름

> 우선순위 범례: 🔴 Must · 🟠 Should · 🟢 Could
> 인증 범례: 🔒 토큰 필요 · 🌐 공개 (상세 기준: `../requirements/auth-classification.md`)
> 참고 문서: 화면 스펙 `../requirements/screen-feature-spec.md`, TourAPI 매핑 `../requirements/tourapi-field-mapping.md`

---

## 도메인 1. 인증 (Authentication)

### US-AUTH-1. 소셜 로그인 🌐 🔴 Must
> **As a** 러너, **I want to** 카카오 또는 애플 계정으로 로그인, **so that** 별도 가입 없이 앱을 이용할 수 있다.

**Acceptance Criteria**
- **Given** 유효한 카카오/애플 인가 정보가 주어지고, **When** 소셜 로그인을 요청하면, **Then** 서버는 사용자 계정을 생성하거나 조회하고 Access/Refresh 토큰을 발급한다.
- **Given** 최초 로그인 사용자라면, **When** 소셜 로그인에 성공하면, **Then** 신규 사용자 레코드가 생성되고 온보딩 필요 여부를 함께 반환한다.
- **Given** 유효하지 않은 인가 정보라면, **When** 로그인을 요청하면, **Then** 인증 실패를 401로 반환한다.

**참조**: FR-1.1 · 화면 V1(직후 온보딩)

---

### US-AUTH-2. 토큰 발급 및 만료 정책 🔴 Must
> **As a** 러너, **I want** 로그인 시 Access/Refresh 토큰을 받고, **so that** 인증이 필요한 API를 이용할 수 있다.

**Acceptance Criteria**
- **Given** 로그인에 성공하면, **When** 토큰이 발급되면, **Then** Access Token 만료는 24시간, Refresh Token 만료는 7일로 설정된다.
- **Given** Refresh Token이 발급되면, **When** 저장이 이뤄지면, **Then** Refresh Token은 Redis에 저장된다.

**참조**: FR-1.2, FR-1.3

---

### US-AUTH-3. 토큰 갱신 🌐 🔴 Must
> **As a** 러너, **I want to** Access Token이 만료되면 Refresh Token으로 재발급, **so that** 다시 로그인하지 않고 계속 이용할 수 있다.

**Acceptance Criteria**
- **Given** 유효한 Refresh Token이 주어지고, **When** 토큰 갱신을 요청하면, **Then** 새 Access Token을 발급한다.
- **Given** Refresh Token이 만료/무효라면, **When** 갱신을 요청하면, **Then** 401을 반환하고 재로그인을 유도한다.
- **Given** 갱신 요청 시, **When** Redis에 저장된 Refresh Token과 대조하면, **Then** 일치하지 않으면 거부한다.

**참조**: FR-1.2

---

### US-AUTH-4. 인증 미들웨어 / 보호 엔드포인트 🔴 Must
> **As a** 시스템, **I want to** 보호된 API에 유효한 토큰을 요구하고 공개 엔드포인트만 예외 허용, **so that** 인가된 사용자만 개인 리소스에 접근한다.

**Acceptance Criteria**
- **Given** deny-by-default 정책에서, **When** 보호된 엔드포인트에 토큰 없이 접근하면, **Then** 401을 반환한다.
- **Given** 명시적 공개 화이트리스트(로그인/갱신·지역·코스조회·장소조회)라면, **When** 토큰 없이 접근하면, **Then** 정상 처리한다.
- **Given** 🔒 소유 리소스(러닝기록 등)에 대해, **When** 토큰의 사용자와 리소스 소유자가 다르면, **Then** 403/404로 거부한다.

**참조**: FR-1.2, NFR-1.1, NFR-1.2 · `auth-classification.md`

---

### US-AUTH-5. 로그아웃 🔒 🔴 Must
> **As a** 러너, **I want to** 로그아웃, **so that** 내 세션(토큰)을 무효화할 수 있다.

**Acceptance Criteria**
- **Given** 로그인한 사용자가, **When** 로그아웃을 요청하면, **Then** 서버의 Refresh Token을 무효화하고 204를 반환한다.

**참조**: FR-1.2 · 화면 V13

---

## 도메인 2. 사용자 / 프로필 (User & Profile)

### US-USER-1. 온보딩 신체정보 저장 🔒 🔴 Must
> **As a** 러너, **I want to** 온보딩에서 키·체중·성별을 저장(또는 건너뛰기), **so that** 러닝 계산·통계에 활용된다.

**Acceptance Criteria**
- **Given** 로그인 직후 사용자가, **When** 키·체중·성별을 입력해 저장하면, **Then** 프로필에 신체정보가 저장된다.
- **Given** 사용자가 온보딩을 건너뛰면, **When** 저장 없이 진행하면, **Then** 신체정보는 미입력(nullable)로 남고 이후 설정에서 입력 가능하다.

**참조**: 화면 V1 · 신규 도메인

---

### US-USER-2. 프로필 조회 및 수정 🔒 🟠 Should
> **As a** 러너, **I want to** 내 프로필(키·체중·성별)을 조회·수정, **so that** 설정에서 정보를 관리한다.

**Acceptance Criteria**
- **Given** 로그인한 사용자가, **When** 내 프로필을 요청하면, **Then** 본인 프로필 정보를 반환한다.
- **Given** 수정할 필드가 주어지면, **When** 프로필 수정을 요청하면, **Then** 해당 필드만 갱신하고 결과를 반환한다.

**참조**: 화면 V13 · 신규 도메인
> ⏸️ 프로필 사진 업로드/삭제는 저장방식 미정 → 보류 백로그

---

### US-USER-3. 계정 삭제 🔒 🟠 Should
> **As a** 러너, **I want to** 계정을 삭제(탈퇴), **so that** 내 데이터를 제거할 수 있다.

**Acceptance Criteria**
- **Given** 로그인한 사용자가, **When** 계정 삭제를 요청하면, **Then** 사용자와 연관 데이터를 삭제/비활성화하고 204를 반환한다.

**참조**: 화면 V13 · 신규 도메인

---

## 도메인 3. 지역 (Region)

### US-REGION-1. 지역 목록 조회 🌐 🔴 Must
> **As a** 러너, **I want to** 지역 목록(군산/전주)을 조회, **so that** 지역을 선택해 코스를 볼 수 있다.

**Acceptance Criteria**
- **Given** 앱 메인에서, **When** 지역 목록을 요청하면, **Then** 지원 지역(군산/전주 등) 목록을 반환한다.
- **Given** 지역은 TourAPI 법정동 코드와 매핑되고(전북 52 / 군산 130 / 전주 110), **When** 지역이 선택되면, **Then** 후속 조회에서 해당 코드로 필터한다.

**참조**: 화면 V2 · `tourapi-field-mapping.md` §4 · 신규 도메인

---

## 도메인 4. 코스 (Course)

### US-COURSE-1. 지역별 공식 코스 목록 조회 🌐 🔴 Must
> **As a** 러너, **I want to** 선택한 지역의 추천 코스 목록을 조회, **so that** 달릴 코스를 고를 수 있다.

**Acceptance Criteria**
- **Given** 지역(및 선택적 거리 필터)이 주어지면, **When** 코스 목록을 요청하면, **Then** 코스 요약(이름·시간·거리·코스 설명)을 리스트로 반환한다.
- **Given** 조건에 맞는 코스가 없으면, **When** 목록을 요청하면, **Then** 빈 목록을 200으로 반환한다.

**참조**: FR-2.1 · 화면 V2 · `data/courses/courses.json`

---

### US-COURSE-2. 코스 상세 조회 🌐 🔴 Must
> **As a** 러너, **I want to** 코스 상세(경로 좌표·누적거리·경유지)를 조회, **so that** 코스를 지도에서 확인하고 선택한다.

**Acceptance Criteria**
- **Given** 유효한 코스 ID가 주어지면, **When** 상세를 요청하면, **Then** polyline 좌표·누적거리·경유지(waypointAnchors)·총거리를 반환한다.
- **Given** 존재하지 않는 코스 ID라면, **When** 상세를 요청하면, **Then** 404를 반환한다.

**참조**: FR-2.1, FR-2.3 · 화면 V8

---

### US-COURSE-3. 사용자 코스 직접 생성 🔒 🟠 Should
> **As a** 러너, **I want to** 내가 만든 경로(출발/경유/도착 + Tmap JSON)를 코스로 저장, **so that** 원하는 코스를 남기고 다시 달릴 수 있다.

**Acceptance Criteria**
- **Given** 프론트가 Tmap으로 생성한 경로 JSON(좌표·거리)이 주어지면, **When** 코스 생성을 요청하면, **Then** 좌표/거리를 저장하고 생성된 코스를 반환한다. (백엔드는 Tmap 호출 안 함)
- **Given** 좌표가 비었거나 유효하지 않으면, **When** 생성을 요청하면, **Then** 400을 반환한다.

**참조**: FR-2.2, FR-2.3 · 화면 V7

---

## 도메인 5. 러닝 기록 (Run Record)

### US-RUN-1. 완료된 러닝 기록 저장 🔒 🔴 Must
> **As a** 러너, **I want to** 완주/중단한 러닝의 완료 데이터를 저장, **so that** 나중에 기록을 확인할 수 있다.

**Acceptance Criteria**
- **Given** 클라이언트가 추적한 완료 데이터(경로·거리·시간·페이스, 코스 참조 optional)가 주어지면, **When** 저장을 요청하면, **Then** 사용자와 연결해 기록을 저장한다. (실시간 소켓 없음)
- **Given** 코스 기반 러닝이면, **When** 저장하면, **Then** 완주율/통계 산출용 데이터를 함께 보관한다. (완주율·통계 계산 기준은 보류 백로그)
- **Given** 필수 필드가 누락되면, **When** 저장을 요청하면, **Then** 400을 반환한다.

**참조**: FR-3.1, FR-3.2 · 화면 V9/V10

---

### US-RUN-2. 러닝 기록 목록 조회 🔒 🟠 Should
> **As a** 러너, **I want to** 내 러닝 기록 목록을 조회, **so that** 지난 러닝을 되돌아본다.

**Acceptance Criteria**
- **Given** 로그인한 사용자에게 기록이 있으면, **When** 기록 목록(기간 필터 선택)을 요청하면, **Then** 본인 기록만 반환한다.

**참조**: FR-3.3 · 화면 V11

---

### US-RUN-3. 러닝 기록 상세 조회 🔒 🟠 Should
> **As a** 러너, **I want to** 특정 러닝 기록의 상세(날짜·시간·거리·페이스·경로)를 조회, **so that** 상세 내용을 확인한다.

**Acceptance Criteria**
- **Given** 본인 기록 ID가 주어지면, **When** 상세를 요청하면, **Then** 경로·거리·시간·페이스 등 상세를 반환한다.
- **Given** 타인의 기록 ID라면, **When** 상세를 요청하면, **Then** 403/404로 거부한다.

**참조**: FR-3.3 · 화면 V12

---

## 도메인 6. 장소 (Places — TourAPI 실시간 프록시)

> 모든 장소 조회는 TourAPI(KorService2)를 **실시간 프록시**하여 제공. 상세는 여러 오퍼레이션을 조합. 매핑: `tourapi-field-mapping.md`.

### US-PLACE-1. 키워드 장소 검색 🌐 🔴 Must
> **As a** 러너, **I want to** 키워드로 장소를 검색, **so that** 원하는 장소를 찾는다.

**Acceptance Criteria**
- **Given** 검색 키워드(및 지역/카테고리 선택)가 주어지면, **When** 검색을 요청하면, **Then** `searchKeyword2` 기반 장소 목록을 반환한다.
- **Given** 결과가 없으면, **When** 검색하면, **Then** 빈 목록을 반환한다.

**참조**: 화면 V4/V5 · TourAPI `searchKeyword2`

---

### US-PLACE-2. 지역/현위치 기반 장소 목록 조회 🌐 🔴 Must
> **As a** 러너, **I want to** 지도에서 지역 또는 현위치 기반 관광지·음식점·편의시설 목록을 조회, **so that** 주변 장소를 파악한다.

**Acceptance Criteria**
- **Given** 지역 코드가 주어지면, **When** 목록을 요청하면, **Then** `areaBasedList2` 기반 장소 목록(좌표 포함)을 반환한다.
- **Given** 현위치 좌표가 주어지면, **When** 추천 목록을 요청하면, **Then** `locationBasedList2` 기반 근접 장소 목록을 반환한다.

**참조**: 화면 V3 · TourAPI `areaBasedList2`/`locationBasedList2`

---

### US-PLACE-3. 주변 장소 조회 (반경) 🌐 🔴 Must
> **As a** 러너, **I want to** 특정 지점 반경(예: 500m) 내 편의시설·음식점·관광지를 조회, **so that** 코스 주변/완주 지점 주변 정보를 활용한다.

**Acceptance Criteria**
- **Given** 좌표와 반경(m)이 주어지면, **When** 주변 장소를 요청하면, **Then** `locationBasedList2`로 반경 내 장소를 거리(`dist`)와 함께 반환한다.
- **Given** 카테고리 필터가 주어지면, **When** 요청하면, **Then** 해당 타입(관광지/음식점/편의시설 등)으로 필터한다.

**참조**: 화면 V8/V10 · TourAPI `locationBasedList2`
> ⏸️ 기본 반경(먹거리 1km vs 완주 500m) 통일은 보류 백로그 — 현재 반경은 요청 파라미터로 처리

---

### US-PLACE-4. 장소 상세 조회 🌐 🔴 Must
> **As a** 러너, **I want to** 장소의 상세(개요·영업시간·이미지 등)를 조회, **so that** 방문 여부를 판단한다.

**Acceptance Criteria**
- **Given** 장소 ID(contentid)와 타입(contentTypeId)이 주어지면, **When** 상세를 요청하면, **Then** `detailCommon2`(개요)+`detailIntro2`(소개)+`detailInfo2`(부가)+`detailImage2`(이미지)를 조합해 반환한다.
- **Given** 장소 타입이 관광지(12) vs 음식점(39)이면, **When** 소개정보를 매핑하면, **Then** 타입별 필드셋을 분기해 공통 필드로 정규화한다.
- **Given** 응답 필드가 문자열/빈값이면, **When** 반환하면, **Then** 숫자 파싱·빈값 null 정규화를 적용한다.

**참조**: 화면 V3/V5 · TourAPI `detailCommon2`/`detailIntro2`/`detailInfo2`/`detailImage2`

---

## 도메인 7. 배지 (Badge)

### US-BADGE-1. 모범음식점 / 착한가격업소 배지 표시 🌐 🟠 Should
> **As a** 러너, **I want to** 장소에 모범음식점/착한가격업소 배지를 보고, **so that** 믿을 만한 가게를 구분한다.

**Acceptance Criteria**
- **Given** 장소가 배지 데이터(요식업만 적재)와 매칭되면, **When** 장소 목록/상세를 반환하면, **Then** `badges`(MODEL_RESTAURANT/GOOD_PRICE)를 포함한다.
- **Given** 인허가번호·좌표가 없어 업소명+주소로 매칭할 때, **When** 자동 매칭이 애매하면, **Then** 하이브리드(수동 확정) 매핑 결과를 사용한다.
- **Given** 매칭되는 배지가 없으면, **When** 반환하면, **Then** 빈 배열로 정상 동작한다.

**참조**: FR-4.2, FR-4.4 · `data/file-data/badge-data-analysis.md`

---

## 도메인 8. 공통 / 기반 (Common & Foundation)

### US-COMMON-1. 공통 응답 래퍼 🔴 Must
> **As a** 클라이언트 개발자, **I want** 일관된 성공/실패 응답 구조, **so that** 응답을 예측 가능하게 처리한다.

**Acceptance Criteria**
- **Given** 성공 응답에 데이터가 있으면, **When** 응답을 보내면, **Then** 데이터를 담아 200으로 반환한다.
- **Given** 반환할 본문이 없으면, **When** 응답을 보내면, **Then** 204를 반환한다.
- **Given** 실패가 발생하면, **When** 응답을 보내면, **Then** 상황에 맞는 HTTP status와 실패 구조를 반환한다.

**참조**: FR-6.1

---

### US-COMMON-2. 전역 예외 처리 🔴 Must
> **As a** 시스템, **I want to** 예외를 일관되게 처리, **so that** 클라이언트가 안정적으로 오류를 다룬다.

**Acceptance Criteria**
- **Given** 처리되지 않은 예외가 발생하면, **When** 요청이 실패하면, **Then** 전역 예외 처리기가 공통 실패 응답 + 적절한 HTTP status로 변환한다.
- **Given** 검증 오류가 발생하면, **When** 요청이 실패하면, **Then** 400과 함께 필드 오류 정보를 반환한다.

**참조**: FR-6.2

---

### US-COMMON-3. 설정 정리 (로거 + JWT 라이브러리) 🔴 Must
> **As a** 개발자, **I want** 초기 설정 이슈를 정리, **so that** 안정적으로 인증/로깅을 구성한다.

**Acceptance Criteria**
- **Given** 로거 설정이 잘못돼 있으면, **When** 설정을 수정하면, **Then** 올바른 패키지로 교정된다.
- **Given** JWT 라이브러리가 build.gradle에 없으면, **When** 의존성을 추가하면, **Then** JWT 발급/검증이 가능해진다.

**참조**: NFR-5.1, NFR-5.2

---

### US-COMMON-4. TourAPI 연동 클라이언트 (실시간 프록시) 🔴 Must
> **As a** 시스템, **I want** TourAPI를 안정적으로 실시간 호출하는 연동 계층, **so that** 장소 기능이 외부 장애에 흔들리지 않는다.

**Acceptance Criteria**
- **Given** TourAPI 실시간 프록시 방식에서, **When** 외부 호출이 지연/실패하면, **Then** 타임아웃·재시도·서킷브레이커로 보호하고 적절한 실패 응답을 반환한다.
- **Given** 상세가 여러 오퍼레이션을 필요로 하면, **When** 상세를 조회하면, **Then** 병렬 호출로 지연을 최소화한다.
- **Given** 반복 요청이 있으면, **When** 응답을 처리하면, **Then** 단기 캐시로 외부 부하/레이트리밋을 완화한다.

**참조**: `tourapi-field-mapping.md` §5 · 신규 기반 스토리

---

## 스토리 ↔ 페르소나 매핑

| 스토리 | 페르소나 | 인증 | 우선순위 |
|---|---|---|---|
| US-AUTH-1, 3 | 러너 (P1) | 🌐 | Must |
| US-AUTH-2, 4 | 러너/시스템 | — | Must |
| US-AUTH-5 | 러너 (P1) | 🔒 | Must |
| US-USER-1 | 러너 (P1) | 🔒 | Must |
| US-USER-2, 3 | 러너 (P1) | 🔒 | Should |
| US-REGION-1 | 러너 (P1) | 🌐 | Must |
| US-COURSE-1, 2 | 러너 (P1) | 🌐 | Must |
| US-COURSE-3 | 러너 (P1) | 🔒 | Should |
| US-RUN-1 | 러너 (P1) | 🔒 | Must |
| US-RUN-2, 3 | 러너 (P1) | 🔒 | Should |
| US-PLACE-1~4 | 러너 (P1) | 🌐 | Must |
| US-BADGE-1 | 러너 (P1) | 🌐 | Should |
| US-COMMON-1~4 | (기반) | — | Must |

## 요구사항 / 화면 커버리지

| 요구사항 | 스토리 |
|---|---|
| FR-1 인증 | US-AUTH-1~5 |
| FR-2 코스 | US-COURSE-1~3 |
| FR-3 러닝 기록 | US-RUN-1~3 |
| FR-4 추천/장소/배지 | US-PLACE-1~4, US-BADGE-1 |
| FR-6 공통 | US-COMMON-1~2 |
| NFR-1 보안 | US-AUTH-4 |
| NFR-5 설정 정리 | US-COMMON-3 |

| 화면 | 스토리 |
|---|---|
| V1 온보딩 | US-AUTH-1, US-USER-1 |
| V2 메인 | US-REGION-1, US-COURSE-1 |
| V3 지도 | US-PLACE-2, US-PLACE-4 |
| V4/V5 검색 | US-PLACE-1, US-PLACE-4 |
| V6 위치정보 | (배제) |
| V7 경로수정 | US-COURSE-3 |
| V8 코스확인 | US-COURSE-2, US-PLACE-3 |
| V9 코스진행 | (백엔드 없음) |
| V10 완주결과 | US-RUN-1, US-PLACE-3 |
| V11 기록 | US-RUN-2 |
| V12 기록상세 | US-RUN-3 |
| V13 설정 | US-AUTH-5, US-USER-2, US-USER-3 |
| V14 업적 | (보류) |

---

## 보류 백로그 (회의 후 확정 — 스토리화 대기)

아직 결정/기준이 미정이라 인수조건 확정 불가 → 완성 스토리에서 제외하고 목록만 유지.

| 항목 | 화면 | 미결 사유 |
|---|---|---|
| 완주율 계산 기준 | V10 | 계산식 미정 (거리 대비 등) |
| 러닝 통계 집계 (시간/페이스/칼로리/거리) | V10/V11 | 집계 항목·주간/월간/연간 기준 미정, 칼로리 산식 미정 |
| 공유용 결과 데이터 | V10/V12 | 형식 미정 |
| 유사 검색어 / 최근 검색어 | V4/V5 | 서버 저장 vs 클라 로컬 미정 |
| 프로필 사진 업로드/삭제 | V13 | 이미지 저장소(S3 등) 미정 |
| 이용약관 / 개인정보 처리방침 | V13 | 제공 주체(API vs 앱 정적) 미정 |
| 업적 (목록/상세) | V14 | 도메인 전체 미정 |
| 근접 추천 반경 기본값 통일 (1km/500m) | V3/V10 | 화면별 파라미터로 처리 중, 기본값 미정 |
| 편의시설(공중화장실 등) 좌표·노출 | V3/V8/V10 | ⏸️ 후순위. 화장실 CSV에 좌표 없음 → 지오코딩 필요(`data/file-data/toilet-data-analysis.md`) |
