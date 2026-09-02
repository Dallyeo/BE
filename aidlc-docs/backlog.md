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

## 🟡 업로드 이미지 — 로컬 디스크 저장의 운영 리스크 (2026-09-02 도입)
러닝 기록 이미지를 **EC2 로컬 디스크**에 저장하도록 했습니다(프론트에 업로드용 스토리지가 없어서 선택).
동작에는 문제가 없지만 아래는 인지하고 있어야 합니다.
- **배포 전 필수 설정**: `/etc/dallyeo/dallyeo.env` 에 `UPLOAD_DIR` 을 **절대경로**로 지정할 것.
  systemd 유닛에 `WorkingDirectory` 가 없어서 기본값 `./uploads` 는 `/uploads` 로 해석됩니다.
  `sudo mkdir -p /var/lib/dallyeo/uploads && sudo chown dallyeo:dallyeo /var/lib/dallyeo/uploads`
- **nginx**: `client_max_body_size 12m` 필요(기본 1MB면 10MB 업로드가 413). certbot이 443 블록을
  자동 생성하면 **그 블록에도 다시 추가**해야 합니다.
- **백업 없음**: DB 백업에 이미지가 포함되지 않습니다. 인스턴스 교체/디스크 장애 시 유실됩니다.
- **디스크 증가 무제한**: 파일 1개 10MB 상한만 있고 사용자별 총량/보관기간 제한이 없습니다. 모니터링 필요.
- **삭제 API 없음**: 재업로드는 교체(이전 파일 자동 삭제)되지만, 러닝 기록 삭제·계정 탈퇴 시 이미지 파일은
  남습니다(고아 파일). 러닝 삭제 API를 만들 때 함께 정리해야 합니다.
- **TODO(전환)**: 트래픽/용량이 늘면 S3 + presigned URL 로 이전. 그때 `ImageStorage` 만 교체하면 되도록
  저장 로직은 이미 한 클래스에 격리해 두었습니다.

## 🟡 코스 이미지 — 파일 수급 대기 (2026-09-02)
`course.image_url` 컬럼·응답 노출·정적 서빙(`/images/courses/**`)은 완료. **이미지 파일만 받으면 됩니다.**
- 받은 뒤 할 일: ① `src/main/resources/static/images/courses/` 에 파일 배치
  ② `courses.json` 각 코스에 `"imageUrl": "/images/courses/{파일명}"` 기입
  ③ 이미 시드된 운영 DB용 백필 SQL 작성(시드 로더는 기존 행을 갱신하지 않음)
- 상세 규약: `src/main/resources/static/images/courses/README.md`

## 🟡 배지 매칭률 (프론트 문의 회신 사항, 2026-09-02)
목록에도 `badges` 를 실었지만, **매칭되는 장소 자체가 적다**는 점은 그대로입니다.
- 원천 데이터: 군산시 공공데이터 2종 — 모범음식점 52건 + 착한가격업소 60건(미용업 등 비요식업 포함), 총 97건 적재.
- 매칭 규칙: 정규화한 **업소명 AND 주소가 둘 다 완전일치**(BR-U3-8, 오매칭 방지 목적의 보수적 규칙).
  TourAPI 표기가 조금만 달라도 탈락하므로 히트율이 낮습니다. 데이터 부족이 아니라 규칙 때문입니다.
- **TODO(선택)**: 히트율을 올리려면 ① 업소명만 일치 + 좌표 근접(수백 m) 보조 검증, ② 도로명 주소 파싱 후 비교,
  ③ 유사도(편집거리) 임계값 매칭 중 택일. 오매칭(엉뚱한 가게에 배지) 리스크와 트레이드오프라 결정 필요.

## 기타 (기존 defer)
- 레이트리밋(공개 API 남용/스크래핑 방어 — CORS보다 본질적 방어책).
- at-rest 암호화(단일 EC2 로컬디스크 accepted risk), 위치정보(polyline) RDS 이관 시 우선 암호화.
- 러닝 수정/삭제, 완주율 계산, 통계/집계, 러닝 목록 페이징, polyline 크기 상한.
- 업적 진행률(%) 표시, 계정삭제 시 run/user_achievement cascade 정책(**+ 업로드 이미지 파일 정리 포함**).
- JaCoCo 커버리지, 자동 통합/부하/보안 스캔 도입.
