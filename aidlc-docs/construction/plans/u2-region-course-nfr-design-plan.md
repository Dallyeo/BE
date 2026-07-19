# NFR Design Plan (Light) — U2 지역·코스조회 🌐

NFR 요구를 설계 패턴/논리 컴포넌트로 구체화. U2는 외부 회복성·캐시가 없어 **매우 경량** — JPA 조회 패턴 + JSON 컬럼 변환 + 인덱스만.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **확정(이미)**: 캐시 미적용(R1), region/measuredCategory 인덱스(R2), U1-a 정책 상속(R3), JSON TEXT 컬럼(Q1).

---

## Part A. 실행 체크리스트
- [x] 결정 질문 수집(Part B)
- [x] `nfr-design-patterns.md` — 영속/조회/검증 패턴
- [x] `logical-components.md` — Course 엔티티/Repository/Service/Loader 배선

---

## Part B. 결정 질문

### Question D1 — JSON 컬럼 매핑 방식
polyline/cumulativeMeters/waypointAnchors(JSON TEXT)를 엔티티에 어떻게 매핑?

A) **JPA AttributeConverter(@Convert)** — 엔티티 필드는 객체(List<PolylinePoint> 등), 컨버터가 JSON↔객체 자동 변환 (권장, 엔티티 깔끔)
B) **서비스레벨 직렬화** — 엔티티 필드는 String(JSON), 서비스에서 ObjectMapper로 수동 직렬화/역직렬화
C) 위임
X) Other

[Answer]: A

---

### Question D2 — waypointCount 산출 방식
목록 요약의 `waypointCount`를 어떻게?

A) **조회 시 계산** — waypointAnchors JSON 길이로 산출(목록에선 역직렬화 필요) 또는
B) **저장 컬럼** — 적재 시 `waypointCount`를 별도 컬럼에 저장(목록 조회 시 JSON 미역직렬화, 성능) (권장)
C) 위임
X) Other

[Answer]: B

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u2-region-course/nfr-design/nfr-design-patterns.md`
- [x] `aidlc-docs/construction/u2-region-course/nfr-design/logical-components.md`
