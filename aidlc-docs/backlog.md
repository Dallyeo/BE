# Backlog / TODO

프로덕션 반영 후 다듬어야 할 항목 모음.

## 🔴 보안 — CORS 좁히기 (임시 전체 허용 상태)
- **현재 상태(2026-08-23)**: iOS 웹뷰 CORS 차단을 우선 해제하려고 `SecurityConfig.corsConfigurationSource()`에서 **전체 오리진 허용**(`allowedOriginPatterns("*")`, `allowCredentials(false)`) + `OPTIONS /** permitAll` 적용. **테스트 목적의 임시 조치.**
- **왜 임시로 열어도 비교적 안전한가**: 인증이 Bearer 토큰(헤더) 방식 + 쿠키 세션 없음 → `allowCredentials=false` 와일드카드는 CSRF 유발 안 함. 보호 엔드포인트는 토큰 없으면 401.
- **해야 할 일(TODO)**:
  1. 프론트에 **iOS 웹뷰가 실제로 보내는 `Origin` 값** 확인 요청 (예: `capacitor://localhost`, `ionic://localhost`, `null` 등).
  2. `corsConfigurationSource()`의 `setAllowedOriginPatterns`를 실제 값으로 **좁히기**: 예) `https://dallyeo.cloud`, `capacitor://*`, `ionic://*`, `http://localhost:*`.
  3. `allowedHeaders`도 필요한 것만(`Authorization`, `Content-Type`)으로 좁히는 것 고려.
  4. **절대 금지**: `allowCredentials(true)` + 와일드카드/Origin 반사 조합. (쿠키 인증으로 전환 시 CORS 정책 전면 재검토.)
- **위치**: `src/main/java/com/ppip/dallyeo/config/SecurityConfig.java` (⚠️ TEMPORARY 주석 참고).
- **배포 주의**: 이 변경은 **main 병합 → EC2 배포**해야 프로덕션(iOS) 테스트에 반영됨. 로컬에선 preflight 200 + ACAO 헤더 검증 완료.

## 기타 (기존 defer)
- 레이트리밋(공개 API 남용/스크래핑 방어 — CORS보다 본질적 방어책).
- at-rest 암호화(단일 EC2 로컬디스크 accepted risk), 위치정보(polyline) RDS 이관 시 우선 암호화.
- 러닝 수정/삭제, 완주율 계산, 통계/집계, 러닝 목록 페이징, polyline 크기 상한.
- 업적 진행률(%) 표시, 계정삭제 시 run/user_achievement cascade 정책.
- JaCoCo 커버리지, 자동 통합/부하/보안 스캔 도입.
