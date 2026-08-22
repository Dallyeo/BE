# Performance Test Instructions — Dallyeo

## Purpose
부하 하에서의 응답성 확인. 단, NFR 전 유닛에서 **하드 응답시간 타깃 미설정(N2 기조)** — 본 단계는 관측·회귀 감지 위주.

## 현재 상태
- 초기 소규모 서비스, 단일 EC2 코로케이션. 정량 SLA 미확정 → **정식 부하 테스트는 defer(백로그)**.
- 성능 민감 지점(설계상):
  - 러닝 목록(`GET /runs`): `(userId, finishedAt)` 인덱스 + polyline 제외 경량 응답.
  - 러닝 저장/상세: polyline(MEDIUMTEXT) 직렬화 1회.
  - 장소(`/places/*`): 외부 TourAPI 실시간 프록시(캐시+서킷, U3).

## 권장(선택) 스모크 부하
간단 확인이 필요하면 경량 도구로:
```bash
# 예: 공개 코스 목록 100회 반복 응답시간 관측
for i in $(seq 1 100); do curl -s -o /dev/null -w "%{time_total}\n" localhost:8080/courses; done | \
  awk '{s+=$1; n++} END {printf "avg %.3fs over %d\n", s/n, n}'
```

## 향후(백로그) 기준 잡을 때
- 도구: k6 또는 JMeter.
- 후보 타깃(예시, 미확정): 공개 조회 p95 < 300ms(외부 API 제외), 러닝 저장 p95 < 500ms.
- polyline 대용량/목록 무페이징(NFR defer)이 병목 후보 → 규모 확대 시 페이징·크기 상한 우선.

## 결론
- **현 단계 성능 테스트: N/A(정식 defer)**. 회귀 감지용 스모크만 선택적으로 수행.
