# NFR Design Plan — U3 장소·배지 🌐

NFR 요구를 패턴/컴포넌트로 구체화. 회복성/캐시는 U1-a 상속(엔드포인트별 인스턴스 확장). 남은 설계 선택만 확인.
각 `[Answer]:` 뒤에 알파벳(또는 X)을 적어주세요. "완료" 시 산출물을 생성합니다.

> **확정(이미)**: 엔드포인트별 서킷+캐시(N1), 성능 하드타깃 미설정(N2), 배지 복합인덱스+멱등(N3), CSV 파일별 인코딩, Assembler common→intro 순차(P3), 배지 매칭 업소명+주소 둘 다(P5).

---

## Part A. 실행 체크리스트
- [x] 결정 질문 수집(Part B)
- [x] `nfr-design-patterns.md` — 회복성/캐시/조합/정규화 패턴
- [x] `logical-components.md` — Place/Badge 컴포넌트 배선

---

## Part B. 결정 질문

### Question D1 — AddressNormalizer 정규화 규칙 수준 (P5 보수적 반영)
업소명/주소 정규화를 어느 정도로?

A) **보수적** — 모든 공백 제거, 괄호와 그 안 내용(동명 등) 제거, 특수문자(-·,.)제거, 시도명 표기 통일(전북특별자치도=전라북도=전북). 그 이상 파싱 안 함 (권장, 오매칭 방지)
B) **공격적** — 도로명+건물번호만 추출, 지점명 분리 등 적극 파싱(재현율↑, 오매칭 위험↑)
C) 위임
X) Other

[Answer]: A

---

### Question D2 — 배지 조회 호출 위치
PlaceDetail에 badges를 붙이는 호출을 어디서?

A) **PlaceDetailAssembler가 BadgeService 호출** — 상세 조합 책임을 Assembler에 집중(TourAPI 조합 + 배지) (권장)
B) PlaceService가 Assembler 결과에 배지 별도 부착
C) 위임
X) Other

[Answer]: A

---

## Part C. 생성할 산출물
- [x] `aidlc-docs/construction/u3-place-badge/nfr-design/nfr-design-patterns.md`
- [x] `aidlc-docs/construction/u3-place-badge/nfr-design/logical-components.md`
