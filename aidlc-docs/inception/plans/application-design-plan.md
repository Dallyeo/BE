# Application Design Plan — Dallyeo

이 문서는 애플리케이션 설계(고수준 컴포넌트/서비스/의존관계)를 위한 **계획 + 결정 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X로 직접 설명)을 적어주세요. 모두 채운 뒤 "완료"라고 알려주시면 설계 산출물을 생성합니다.

> 상세 비즈니스 로직/데이터 모델은 이후 Functional Design(단위별, CONSTRUCTION)에서 다룹니다. 여기서는 **구조·경계·인터페이스** 수준만 정합니다.

---

## Part A. 설계 실행 체크리스트 (답변 확정 후 실행 — 지금은 참고용)

- [x] 도메인/컴포넌트 식별 및 책임 정의 (`components.md`)
- [x] 컴포넌트 메서드 시그니처 정의 (`component-methods.md`)
- [x] 서비스 계층/오케스트레이션 설계 (`services.md`)
- [x] 컴포넌트 의존관계·통신 패턴 (`component-dependency.md`)
- [x] 통합 설계 문서 (`application-design.md`)
- [x] 설계 완전성·일관성 검증

**확정 답변**: D1=A(도메인 우선), D2=A(단일 Facade), D3=A(백엔드 병렬조합), D4=A(RestClient+CompletableFuture), D5=A(DB 배지테이블+조인), D6=A(Redis 캐시), D7=A(Resilience4j), D8=A(백엔드 소셜검증), D9=A(공통 전부 포함). 기본 패키지: `com.ppip.dallyeo`.

---

## Part B. 설계 결정 질문

### Question D1 — 패키지(코드) 구조
컴포넌트를 어떻게 조직할까요?

A) **도메인 우선** — `com.ppip.auth`, `com.ppip.place`, `com.ppip.course` … 각 도메인 안에 controller/service/repository (도메인 응집도↑, 8도메인에 적합)
B) **레이어 우선** — `com.ppip.controller.*`, `com.ppip.service.*`, `com.ppip.repository.*`
C) **하이브리드** — 공통(common/config)만 레이어, 나머지는 도메인
X) Other

[Answer]: A

---

### Question D2 — TourAPI 연동 컴포넌트 구조
외부 TourAPI(7~9개 오퍼레이션) 연동을 어떻게 구성할까요?

A) **단일 Facade 클라이언트** — `TourApiClient` 하나에 오퍼레이션별 메서드(searchKeyword/areaBasedList/detailCommon…) + 응답 DTO 매핑 분리 (권장)
B) **오퍼레이션별 개별 클라이언트 클래스** 로 분리
C) **목록/상세 2개 클라이언트**로 분리
X) Other

[Answer]: A

---

### Question D3 — 장소 상세 조합(병렬 호출) 방식
장소 상세는 detailCommon2 + detailIntro2 + detailInfo2 + detailImage2 여러 콜을 조합합니다. 어떻게 처리할까요?

A) **백엔드가 병렬 호출 후 조합**해 하나의 상세 응답으로 반환 (프론트 편의↑, 권장)
B) 프론트가 필요한 상세만 개별 엔드포인트로 호출
X) Other

[Answer]: A

---

### Question D4 — 병렬 호출/HTTP 클라이언트 수단
D3에서 병렬 호출 시 어떤 HTTP 클라이언트를 쓸까요? (현재 앱은 Spring MVC 블로킹)

A) **RestClient + CompletableFuture(스레드풀) 병렬** — 기존 MVC와 자연스러움 (권장)
B) **WebClient(리액티브)** 로 비동기 병렬 — 성능↑, 러닝커브↑
C) 순차 호출(단순, 지연 감수) — 초기엔 이걸로
X) Other

[Answer]: A

---

### Question D5 — 배지(모범음식점/착한가격업소) 저장·매칭 위치
하이브리드 매칭(업소명+주소, 요식업만) 결과를 어떻게 둘까요?

A) **DB 배지 테이블에 적재** → 장소 조회 시 정규화 키(업소명+주소)로 조인 (권장)
B) **앱 구동 시 인메모리 로드** 후 매칭
X) Other

[Answer]: A

---

### Question D6 — TourAPI 응답 캐시 수단
실시간 프록시 부하/레이트리밋 완화를 위한 단기 캐시는?

A) **Redis(이미 사용 중) 단기 캐시** (권장 — 인프라 재사용)
B) **로컬 인메모리(Caffeine)**
C) **캐시 없음** — 초기엔 생략, 나중에 추가
X) Other

[Answer]: A

---

### Question D7 — 외부 연동 회복성(타임아웃/재시도/서킷브레이커)
US-COMMON-4의 회복성은 어떻게 구현할까요?

A) **Resilience4j** 도입 (타임아웃+재시도+서킷브레이커 표준, 권장)
B) **타임아웃만** 우선(RestClient 설정), 서킷브레이커는 이후
C) 아무것도 안 함(초기 단순화)
X) Other

[Answer]: A

---

### Question D8 — 소셜 로그인 검증 주체
카카오/애플 로그인에서 인가 검증을 누가 하나요?

A) **백엔드가 검증** — 프론트가 인가코드/identity token 전달 → 백엔드가 카카오/애플에 검증·사용자정보 조회 (표준, 권장)
B) **프론트가 검증** 후 사용자정보만 백엔드에 전달
X) Other

[Answer]: A

---

### Question D9 — 공통 컴포넌트 범위
공통·기반(Unit 1)에 넣을 것을 확인해주세요. (기본 전부 포함 권장)

A) **전부 포함** — 공통 응답 래퍼, 전역 예외 처리, Security(공개 화이트리스트+JWT 필터), TourAPI 클라이언트, 캐시/회복성 설정 (권장)
B) 일부만 — (X에 제외할 항목 기재)
X) Other

[Answer]: A

---

## Part C. 생성할 산출물 (확정 후)
- [x] `aidlc-docs/inception/application-design/components.md`
- [x] `aidlc-docs/inception/application-design/component-methods.md`
- [x] `aidlc-docs/inception/application-design/services.md`
- [x] `aidlc-docs/inception/application-design/component-dependency.md`
- [x] `aidlc-docs/inception/application-design/application-design.md` (통합)
