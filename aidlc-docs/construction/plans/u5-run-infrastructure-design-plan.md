# U5 러닝기록 🔒 — Infrastructure Design Plan (light)

> **유닛**: U5 (run 도메인만). 기존 토폴로지(단일 EC2 코로케이션) 전면 상속.
> **신규 인프라**: `run` 테이블 1개 추가(ddl-auto=update). 그 외 없음.

---

## 실행 체크리스트

- [x] Step 1: 설계 산출물 분석 (FD/NFR Design)
- [x] Step 2: 본 계획 작성
- [x] Step 3: 질문 검토 — **미결 없음**(신규 시크릿/egress/큐/캐시 없음 → 질문 생략)
- [x] Step 4: 계획 저장
- [x] Step 5: (해당 없음)
- [x] Step 6: infrastructure-design.md / deployment-architecture.md 작성
- [x] Step 7: 완료 메시지 및 승인 대기

## 질문
없음. 근거: 컴퓨트/네트워크/모니터링/공유인프라 모두 U1-a~U4 상속. 스토리지만 `run` 테이블 추가(기존 MySQL, ddl-auto=update). 외부 호출·메시징·신규 시크릿 없음.
