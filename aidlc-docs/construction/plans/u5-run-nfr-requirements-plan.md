# U5 러닝기록 🔒 — NFR Requirements Plan (light)

> **유닛**: U5 (run 도메인만). 사용자 코스 생성 제거됨.
> **기조**: 대부분 U1-a/U4 NFR·인프라·보안 상속. 신규 외부호출/시크릿/기술스택 없음.

---

## 실행 체크리스트

- [x] Step 1: Functional Design 분석 (run 저장/목록/상세)
- [x] Step 2: 본 계획 + 질문 작성
- [x] Step 3: 질문 답변 수집
- [x] Step 4: 계획 저장
- [x] Step 5: 답변 분석(모호성 없음)
- [x] Step 6: nfr-requirements.md / tech-stack-decisions.md 작성
- [x] Step 7: 완료 메시지 및 승인 대기

---

## 질문 및 결정

### NQ1. polyline(GPS 좌표) 저장 크기 제한
- A. 상한 없음(defer) — 그대로 저장, 필요 시 백로그 — **채택**
- B. 점 개수 상한 설정(초과 400)

[Answer]: A (상한 없음/defer). 단, TEXT(약 64KB)는 긴 러닝에서 잘릴 수 있으므로 **컬럼 타입 MEDIUMTEXT**로 넉넉히(저비용 안전장치, NFR Design/Code에 반영).

### NQ2. 목록 조회 페이지네이션
- A. 페이징 없음(defer) — 기간 필터 범위 전체 최신순, 요약은 polyline 제외 경량 — **채택**
- B. page/size 페이징 추가

[Answer]: A (페이징 없음/defer). 목록 요약 응답은 polyline 미포함(경량). 규모 증가 시 백로그로 페이징 추가.

### NQ3. 기술 스택 신규 요소 여부
[Answer]: 없음. Spring Web/Data JPA/MySQL/공통 응답·예외·보안(U1-a/U4) 전부 상속. 신규 의존성/시크릿/외부 호출 없음.
