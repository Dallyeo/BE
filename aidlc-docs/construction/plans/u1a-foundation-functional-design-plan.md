# Functional Design Plan — U1-a 공개 기반 (Foundation)

U1-a의 상세 설계(기술 계약 중심)를 위한 **계획 + 질문**입니다.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. 모두 채운 뒤 "완료"라고 알려주시면 산출물을 생성합니다.

> **U1-a 범위**: 공통 응답 래퍼, 전역 예외 처리, TourApiClient + 설정(Resilience4j/Redis 캐시), SecurityConfig(전체 permitAll 껍데기), 로거 교정, RegionCodeMapper.
> **이미 확정(참조)**: 응답 래퍼/HTTP status/enum → `../../inception/requirements/api-spec-draft.md` §0 · TourAPI 정규화 → `../../inception/requirements/tourapi-field-mapping.md`.
> 도메인 비즈니스 엔티티는 거의 없음(기반 유닛). 아래는 계약·규칙 관련 잔여 결정.

---

## Part A. 설계 실행 체크리스트
- [x] 비즈니스 로직 모델 (`business-logic-model.md`) — 응답/예외/TourAPI 조합·정규화 흐름
- [x] 비즈니스 규칙 (`business-rules.md`) — 에러코드 체계, 정규화/매핑 규칙, 캐시/회복성 정책
- [x] 도메인 엔티티 (`domain-entities.md`) — 기반 값 객체/DTO(응답 래퍼, TourAPI DTO, Region)

**확정 답변**: F1=A(문자열 에러코드), F2=A(필드 오류 목록), F3=A(+엔드포인트별 좌표정책: locationBased 좌표필수→제외 / areaBased·searchKeyword 좌표 null 유지), F4=A(+원본 contentTypeId 보존 `PlaceCategory(type, rawContentTypeId)`, 미매핑 시 WARN 로그), F5=A(코드 상수), F6=C(캐시 TTL 30분+).

---

## Part B. 설계 결정 질문

### Question F1 — 에러 코드 체계
실패 응답 `error.code` 형식을 어떻게 할까요?

A) **문자열 도메인 코드** — 예: `AUTH_INVALID_TOKEN`, `COURSE_NOT_FOUND`, `EXTERNAL_API_ERROR` (가독성↑, 프론트 분기 용이, 권장)
B) **숫자 코드** — 예: `1001`
C) **HTTP status만** 사용, 별도 code 없음
X) Other

[Answer]: A

---

### Question F2 — 검증 실패(400) 응답 상세
Bean Validation 실패 시 필드 오류를 어떻게 내려줄까요?

A) **필드별 오류 목록 포함** — `error.details: [{field, message}]` (프론트 폼 처리 용이, 권장)
B) **단일 메시지만** — 첫 오류 메시지 하나
X) Other

[Answer]: A

---

### Question F3 — TourAPI 빈 값/좌표 정규화 (확정 재확인)
`tourapi-field-mapping.md`대로 정규화할까요?

A) **그대로 적용** — 빈 문자열 `""` → `null`, `mapx/mapy/dist` 문자열 → double 파싱, 실패 시 해당 항목 제외/로그 (권장)
B) 다르게 (X에 설명)
X) Other

[Answer]: A
단, "파싱 실패 시 항목 제외" 정책은 엔드포인트별로 구분해주세요.
- locationBasedList2: 좌표가 필수이므로 파싱 실패 시 해당 항목 제외
- areaBasedList2 / searchKeyword2: 좌표는 null로 두고 항목은 유지
  (좌표 없이도 목록 노출 가치가 있음)
---

### Question F4 — 알 수 없는 관광타입/카테고리 처리
`contentTypeId`가 우리가 매핑 안 한 값이거나 분류가 애매하면?

A) **`ETC`(기타) 카테고리로 폴백** + 원본 typeId 보존 (전체 노출 정책과 정합, 권장)
B) 해당 항목 **제외**(응답에서 필터)
X) Other

[Answer]: A
폴백 시 원본 contentTypeId를 응답 모델에 보존해주세요.
예: PlaceCategory(CategoryType type, int rawContentTypeId)

그리고 미매핑 발생 시 WARN 로그를 남기는 것을 business-rules.md에
명시해주세요. 폴백이 조용하면 신규 타입 추가를 인지하지 못합니다.
---

### Question F5 — RegionCatalog 정의 방식
지역(군산/전주 및 법정동 코드) 정의를 어디에 둘까요?

A) **설정/코드 상수로 정의** — 현재 2개 고정, enum + 매핑 상수 (단순, 권장)
B) **DB 테이블**로 관리 — 지역 추가를 데이터로
X) Other

[Answer]: A

---

### Question F6 — TourAPI 캐시 키/TTL 정책
Redis 단기 캐시의 키/만료를 어떻게 잡을까요?

A) **오퍼레이션+파라미터 해시를 키로, TTL 5~10분** (권장 — 실시간성과 부하완화 절충)
B) TTL 더 짧게(1~2분) — 최신성 우선
C) TTL 더 길게(30분+) — 부하 최소 우선
X) Other

[Answer]: C

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u1a-foundation/functional-design/business-logic-model.md`
- [x] `aidlc-docs/construction/u1a-foundation/functional-design/business-rules.md`
- [x] `aidlc-docs/construction/u1a-foundation/functional-design/domain-entities.md`
