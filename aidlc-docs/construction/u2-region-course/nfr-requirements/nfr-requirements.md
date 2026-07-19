# NFR Requirements (Light) — U2 지역·코스조회 🌐

> 확정: R1=A(캐시 안 함), R2=A(인덱스 추가), R3=A(U1-a 정책 상속). U2는 조회+JPA 중심 → 대부분 U1-a 상속.

---

## 1. 확장성 (Scalability)
- U1-a 상속: 소규모 단일 인스턴스, stateless. `course`는 정적 10행(+U5 사용자 코스 소량 증가 예상).

## 2. 성능 (Performance)
- **하드 타깃 미설정**(R3). 정적 소량 데이터라 MySQL 직접 조회로 충분.
- **캐시 미적용**(R1=A): 읽기전용 10행 + U5 쓰기 시 무효화 복잡성 회피. 필요 시 후속 도입.
- **인덱스**(R2=A): `region`, `measuredCategory` 인덱스로 필터 조회 대비(U5 성장 시 효과).

## 3. 가용성·신뢰성 (Availability & Reliability)
- 외부 API 의존 없음(TourAPI 미사용 유닛) → U1-a Resilience4j 불필요.
- 시드 적재 실패 격리: 개별 코스 변환 실패는 WARN+스킵, 앱 기동 지속(BR-U2-7).
- 코스 상세 미존재 → 404 정규화(BR-U2-6).

## 4. 보안 (Security) [Security Baseline]
- **공개 API**: `/regions`, `/courses*`는 permitAll(U1-a SecurityConfig 껍데기 그대로). 🔒 없음.
- **입력 검증**: `region`/`distance` 필터 및 path `id` 검증. 잘못된 enum 값 → 400(BR-U2-4).
- 민감정보 없음(공개 관광 코스 데이터). 시크릿/개인정보 처리 N/A.

## 5. 유지보수·관측성 (Maintainability & Observability)
- U1-a 구조화 로깅 상속. 시드 적재 결과(신규/스킵 수) INFO, 변환 실패 WARN.
- 테스트: 조회/필터/404/시드 멱등 단위·슬라이스 테스트.

## 6. 미적용(N/A)
- 외부 회복성(Resilience4j), 캐시, 오토스케일링/DR, 컴플라이언스 — 현 단계 N/A.
