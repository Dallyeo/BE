# Infrastructure Design Plan (Light) — U3 장소·배지 🌐

U3 인프라는 U1-a/U2 인프라에 **`badge` 테이블 + CSV 리소스 + TourAPI egress 실사용**만 추가. 남은 결정 없음(전부 상속) → 질문 없이 산출물 생성.

## Part A. 실행 체크리스트
- [x] 설계 분석 (상속 확인)
- [x] 결정 질문 — **없음**(U1-a EC2 코로케이션 + U2 ddl-auto=update 상속)
- [x] `infrastructure-design.md`
- [x] `deployment-architecture.md`

## 상속/신규 요약
- 상속: AWS 단일 EC2(app+MySQL+Redis 코로케이션), ddl-auto=update(I1=A, U2), env 시크릿.
- 신규: `badge` 테이블 자동 생성, CSV 2종 classpath 배치, **TourAPI 443 아웃바운드 egress 실사용**(U1-a에서 요건만·U3에서 실호출), Redis 캐시 실사용(TourAPI 응답).

## 생성 산출물
- [x] `aidlc-docs/construction/u3-place-badge/infrastructure-design/infrastructure-design.md`
- [x] `aidlc-docs/construction/u3-place-badge/infrastructure-design/deployment-architecture.md`
