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

## 🔴 결함 — 전주 장소 조회가 전부 0건 (2026-09-02 발견, 기존 결함)
`GET /places?region=JEONJU` 계열이 **항상 빈 배열**을 반환합니다. V3 지도·V4/V5 검색에서 전주는 빈 화면입니다.
- **원인**: `RegionCatalog` 의 `JEONJU=(52, 110)`. TourAPI `areaBasedList2` 에서 **lDongSignguCd=110(전주시)는 등록 데이터가 0건**이고,
  실제 데이터는 자치구 단위에 들어 있습니다.

  | 코드 | 지역 | 전체 | 음식점(39) |
  |---|---|---|---|
  | 110 | 전주시 (**현재 사용**) | **0** | **0** |
  | 111 | 전주시 완산구 | 355 | 69 |
  | 113 | 전주시 덕진구 | 119 | 45 |
  | 130 | 군산시 | 333 | 129 |

- **막힌 지점**: `LDongCode` 가 단일 `(lDongRegnCd, lDongSignguCd)` 쌍이라, 전주를 지원하려면
  한 Region이 **복수 시군구 코드**를 갖고 두 번 호출해 병합하는 구조가 필요합니다. 설계 변경이라 별도 작업으로 잡을 것.
- **결정 필요**: ① 두 구를 병합해 "전주"로 노출 vs ② 완산구/덕진구를 별도 Region으로 분리.
  병합 시 정렬·거리·중복 처리 기준과 TourAPI 호출 2배(캐시로 흡수 가능) 검토 필요.
- **주의**: `locationBasedList`(반경, `/places/nearby`)는 좌표 기반이라 이 문제와 무관하게 정상 동작합니다.

## 🟡 조회 건수 상한 — 카테고리 미지정 시 여전히 잘림 (2026-09-02)
`PlaceService.DEFAULT_ROWS` 를 30 → 200 으로 올렸지만, `contentTypeId` 없이 조회하면(`/places?region=GUNSAN`)
군산 전체 **333건 중 200건**만 옵니다.
- 카테고리를 지정하면(음식점 129건) 전량이 들어와 현재 체감 문제는 없음.
- 근본 해결은 페이지 순회 또는 API 페이징 노출인데, 응답 크기(200건 ≈ 58KB)와 프론트 무한스크롤 설계가 얽혀 있어 함께 결정 필요.
- 참고 측정: 음식점 109건 ≈ 29KB / 전체 200건 ≈ 58KB, 캐시 히트 시 약 21ms.

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

## 🟢 배지 매칭률 — 개선 완료(미배포), 잔여 한계 기록 (2026-09-02)
목록에도 `badges` 를 실었지만, **매칭되는 장소 자체가 적다**는 점은 그대로입니다.
- 원천 데이터: 군산시 공공데이터 2종 — 모범음식점 52건 + 착한가격업소 60건(미용업 등 비요식업 포함), 총 97건 적재.
- 매칭 규칙: 정규화한 **업소명 AND 주소가 둘 다 완전일치**(BR-U3-8, 오매칭 방지 목적의 보수적 규칙).
  TourAPI 표기가 조금만 달라도 탈락하므로 히트율이 낮습니다. 데이터 부족이 아니라 규칙 때문입니다.
- **TODO(선택)**: 히트율을 올리려면 ① 업소명만 일치 + 좌표 근접(수백 m) 보조 검증, ② 도로명 주소 파싱 후 비교,
  ③ 유사도(편집거리) 임계값 매칭 중 택일. 오매칭(엉뚱한 가게에 배지) 리스크와 트레이드오프라 결정 필요.

## 🔴 목록 영업시간 — TourAPI 호출 한도가 실제 제약 (2026-09-09, 실측)
`/places` 목록 3종에 `businessHours`/`openHours` 를 실었습니다(검색 결과 카드 둘째 줄 요구).
**TourAPI 목록 응답에는 영업시간이 없어** 항목마다 `detailIntro2` 를 따로 불러야 합니다.

### 검증 완료(실호출)
- 엔드포인트 13케이스 콜드 캐시 전수: 음식점 108/109, 카페 20/20, 문화 10/10, 숙박 29/29, 쇼핑 56/56,
  관광 86/93, 레포츠 4/12(원천 데이터 없음), 전체 190/200. 오류 0건.
- **서킷 격리 검증**: 콜드 상태에서 대형 목록 6건 동시 폭주 중 상세(`/places/{id}`) 28회 폴링 **28/28 성공, 최대 0.25s**.
  전용 인스턴스 분리 전에는 목록 실패가 상세를 통째로 막았으므로, 이 격리가 이 작업의 핵심입니다.
- **레이트리미터 정상 동작 확인**: 로그의 초당 발사량이 정확히 설정값에 고정됨(25/s로 설정 시 25.0/s).
- 캐시 워밍 확인: 폭주로 0건이던 목록이 재호출 시 108/109로 채워짐.

### ⚠️ 미해결 — 운영 키 한도 재측정 필요
로컬 실측에서 **두 가지 한도**에 모두 걸렸습니다(사유 코드가 다름):
- `reasonCode 23` 초당 한도(418회): **25 req/s를 지속하면 발생**. 짧은 버스트(40건/0.5초 = 78 req/s)는
  통과하므로 문제는 순간 속도가 아니라 **지속 속도**입니다. 안전한 지속 속도를 못 구했습니다 —
  아래 일일 한도가 먼저 소진돼 더 측정할 수 없었습니다. 잠정 **10 req/s**로 낮춰 뒀고 **운영 키에서 재측정 필요**.
- `reasonCode 22` 일일 한도(51회): **한 세션의 테스트로 소진**. 앱에서 1,360회 + 직접 probe 약 200회.
  개발계정 일일 1,000회 수준으로 보입니다.

**이게 이 기능의 진짜 제약입니다.** 목록 1건당 최대 200회 호출이라, 콜드 캐시 조회 5~10번이면 개발계정 하루치가 사라집니다.

### 결정 필요 (택1 또는 조합)
1. **intro 캐시 TTL 연장** — 영업시간은 자주 안 바뀌므로 30분 → 24시간이면 호출량이 대폭 감소. **가장 효과 크고 부작용 작음.**
2. **채우기를 상위 N건으로 제한**(예: 30) — 요청당 호출 수가 예측 가능해짐. 대신 하위 항목 카드는 영업시간이 빔.
3. **`DEFAULT_ROWS` 축소 = 페이징 도입** — 구조 변경이라 프론트 협의 필요.
4. 운영 키가 이미 충분한 한도(운영계정)라면 현행 유지 + 레이트만 재측정.

### 현재 완충 장치
- 전용 회복성 인스턴스 `tourApiDetailIntroBulk`(서킷 + 레이트리미터) — **상세 화면과 격리**. 재시도 없음(429 상황에서 부하 2배).
- 실패 시 예외 대신 `null` + `unless="#result == null"` → 실패는 캐시하지 않아 다음 호출에서 재시도.
- `tourapi.intro-concurrency=8`, `tourapi.intro-budget=4s`, 건별 Redis 캐시(**TTL 24시간**).
- 어떤 실패에도 목록은 항상 200 응답(영업시간만 null).

**참고 — 리플렉션 결함(고침)**: `TourApiClient` fallback 메서드들이 `private` 이라 병렬 호출 시
resilience4j 가 `IllegalAccessException`. 단일 호출에선 안 드러나던 잠복 결함. package-private 로 변경.

**위치**: `place/BusinessHoursEnricher.java`, `common/util/BusinessHoursText.java`,
`external/tourapi/TourApiClient#detailIntroBulk`.

## 🔴 배포 주의 — 업적 컬럼 마이그레이션 필요 (2026-09-13)
업적을 8종 → 21종으로 늘리면서 **운영 DB에 `ALTER TABLE`이 먼저 필요**합니다.
- 원인: `user_achievement.achievement` 가 Hibernate MySQL 방언이 만든 **네이티브 enum 컬럼**(옛 8종 값만 허용)이고,
  `ddl-auto=update` 는 **기존 컬럼 정의를 바꾸지 않습니다.** 코드만 배포하면 신규 업적 저장이
  `Data truncated for column 'achievement'` → **500** 으로 전부 실패합니다(로컬에서 실제로 재현).
- 조치: **`migrate-user-achievement-varchar.sql` 을 배포 전에 실행.** 무중단이고 기존 8종 값은 그대로 유효합니다.
- 재발 방지: 엔티티에 `@JdbcTypeCode(SqlTypes.VARCHAR)` + `length = 40` 을 명시해 앞으로 업적이 늘어도 DDL 변경이 필요 없습니다.

## 🟢 업적 21종 + 결과창 도장 — 완료 (2026-09-13)
시안 전달본(`resources/data/achievements.json`, `README.md`, `achievement_images/`) 기준으로 카탈로그를 21종으로 확장.
- **자동 판정 16종**: 기존 8종 + 선유도 짱 · 장기간 러닝(3시간 초과) · 완주 10회(자유 러닝 포함) ·
  누적 100km · 얼리버드(KST 00:00~08:00 시작) · 아이스크림 러너(KST 12월 완주) ·
  경유지 3개 이상 코스 완주 · 개척자(courseId 없는 러닝).
- **판정 보류 5종**(`Kind.PENDING` — 목록에는 나오되 자동 달성 안 함):
  뚜벅이(기준 페이스 미확정) · 휴식타임(도착지 정보를 앱이 안 보냄) · 천변벚꽃 · 덕진 호수 · 전주 자연생태관(해당 코스 없음).
  기준이 정해지면 `AchievementType` 의 Kind만 바꾸면 됩니다.
- **도장 이미지**: WebP 42장을 `static/images/achievements/` 로 서빙(`/images/achievements/{code 소문자}_on|off.webp`).
  파일명이 코드에서 파생되므로 경로를 따로 저장하지 않습니다. 응답에 `iconOnUrl`/`iconOffUrl`/`category`/`sortOrder` 추가.
- **시간대**: JVM 기본 시간대를 `Asia/Seoul` 로 고정(`DallyeoApplication#useKoreanTime`). 러닝 목록 날짜 필터도
  UTC → 한국 날짜 경계로 교정. 저장 시각은 전부 `Instant` 이고 JDBC URL에 `serverTimezone=Asia/Seoul` 이 이미
  고정돼 있어 **기존 데이터가 밀지 않습니다.**
- 실호출 검증: 카탈로그 21종·정렬·분류, 이미지 42/42 서빙(200, image/webp, 무인증 접근),
  신규 판정 17케이스(경계값 포함), 도장 최초 1회만.

## 기타 (기존 defer)
- 레이트리밋(공개 API 남용/스크래핑 방어 — CORS보다 본질적 방어책).
- at-rest 암호화(단일 EC2 로컬디스크 accepted risk), 위치정보(polyline) RDS 이관 시 우선 암호화.
- 러닝 수정/삭제, 완주율 계산, 통계/집계, 러닝 목록 페이징, polyline 크기 상한.
- 업적 진행률(%) 표시, 계정삭제 시 run/user_achievement cascade 정책(**+ 업로드 이미지 파일 정리 포함**).
- JaCoCo 커버리지, 자동 통합/부하/보안 스캔 도입.
