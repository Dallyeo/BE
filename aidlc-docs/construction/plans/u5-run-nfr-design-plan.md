# U5 러닝기록 🔒 — NFR Design Plan (light)

> **유닛**: U5 (run 도메인만). NFR Requirements(light) 상속.
> **기조**: 신규 인프라 패턴 없음(서킷/캐시/외부호출/큐 부재). run은 인증 소유자 전용 CRUD.

---

## 실행 체크리스트

- [x] Step 1: NFR Requirements 분석 (인덱스/소유권/경량목록/MEDIUMTEXT/보안)
- [x] Step 2: 본 계획 작성
- [x] Step 3: 질문 검토 — **미결 없음**(FD/NFR-Req에서 전부 확정, 신규 패턴 없음 → 질문 생략)
- [x] Step 4: 계획 저장
- [x] Step 5: (해당 없음)
- [x] Step 6: nfr-design-patterns.md / logical-components.md 작성
- [x] Step 7: 완료 메시지 및 승인 대기

## 질문
없음. 근거: 회복성/확장성/성능/보안 패턴 모두 U1-a/U4 상속 또는 NFR-Req에서 결정(인덱스, MEDIUMTEXT, 경량 목록, 소유권 404, deny-by-default). 신규 논리 컴포넌트는 표준 JPA 3계층뿐.
