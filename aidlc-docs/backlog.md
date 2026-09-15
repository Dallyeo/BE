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

## 🟢 해결 — 전주 장소 조회 0건 (2026-09-13 수정)
`GET /places?region=JEONJU` 계열이 항상 빈 배열이던 결함. **318건 정상 반환** 확인.
- 원인: `RegionCatalog` 의 `JEONJU=(52,110)`. TourAPI에서 **110(전주시)은 등록 데이터 0건**이고
  실제 데이터는 자치구 단위 **111(완산구 353건) · 113(덕진구 118건)** 에 들어 있었다.
- 수정: 지역 하나가 **법정동코드를 여러 개** 가질 수 있게 바꿨다(`RegionCatalog.find` → `List<LDongCode>`,
  `RegionCodeMapper.toLDongCodes`). `PlaceService.mergeByRegion` 이 코드마다 TourAPI를 부른 뒤
  contentId로 중복 제거하고 **이름순 재정렬**한다 — 이어붙이기만 하면 "완산구 전체 → 덕진구 전체" 로
  한 지역인데 목록이 두 덩어리가 된다. 자치구가 하나인 군산은 1회 호출 그대로(회귀 없음).
- 검증(실호출): 전주 전체 318 · 음식점 94 · 카페 20 · 관광 75 · 문화 50건, 검색도 두 구 결과가 섞여 나옴.
  단위 테스트 6종(병합·중복제거·정렬·군산 단일호출·검색 병합·region 미지정).
- 남은 것: **전주 전체 조회는 여전히 잘린다** — 완산구 353건 중 `DEFAULT_ROWS`(200)까지만.
  아래 "조회 건수 상한" 항목과 같은 원인이다.

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

## 🟢 코스 이미지 — 10/10 배치 완료 (2026-09-15)
디자인 이미지를 받아 `static/images/courses/{코스id}.png` 로 배치하고 `courses.json` 에 `imageUrl` 기입.
- 받은 파일명이 한글(`짬뽕런.png` 등)이라 **코스 id 기준 영문으로 변경** — 클라이언트가 URL 인코딩을 신경 쓰지 않아도 된다.
- 운영 DB 백필: **`update-course-images.sql`** (시드 로더는 기존 행을 갱신하지 않으므로 필요).
  이미지 파일은 classpath static 이라 JAR 에 포함된다 — 서버에 따로 업로드할 필요 없음.
- 검증: 10장 전부 200 응답, `GET /courses`·`GET /courses/{id}` 모두 `imageUrl` 노출. 누락 없음.

## 🟢 배지 매칭률 — 개선 완료(미배포), 잔여 한계 기록 (2026-09-02)
목록에도 `badges` 를 실었지만, **매칭되는 장소 자체가 적다**는 점은 그대로입니다.
- 원천 데이터: 군산시 공공데이터 2종 — 모범음식점 52건 + 착한가격업소 60건(미용업 등 비요식업 포함), 총 97건 적재.
- 매칭 규칙: 정규화한 **업소명 AND 주소가 둘 다 완전일치**(BR-U3-8, 오매칭 방지 목적의 보수적 규칙).
  TourAPI 표기가 조금만 달라도 탈락하므로 히트율이 낮습니다. 데이터 부족이 아니라 규칙 때문입니다.
- **TODO(선택)**: 히트율을 올리려면 ① 업소명만 일치 + 좌표 근접(수백 m) 보조 검증, ② 도로명 주소 파싱 후 비교,
  ③ 유사도(편집거리) 임계값 매칭 중 택일. 오매칭(엉뚱한 가게에 배지) 리스크와 트레이드오프라 결정 필요.

## 🟢 목록 영업시간 — DB 보관으로 호출 한도 해결 (2026-09-13)
`/places` 목록 3종의 `businessHours`/`openHours`. TourAPI 목록 응답에 영업시간이 없어
장소마다 `detailIntro2` 를 불러야 하는데, **캐시 TTL로만 버티면 만료될 때마다 전 장소를 다시 받아
일일 호출 한도를 매일 소진**했다(실측 ~800회/일). 영업시간은 몇 달에 한 번 바뀔까 말까 한 값인데도.

**해결**: `place_business_hours` 테이블에 영구 보관(`BusinessHoursStore`). 조회 순서는
**보관소 → 없는 것만 TourAPI → 받은 값 보관**. 재조회는 `tourapi.hours-refresh-after`(기본 30일)가 지난 것만.
- 영업시간이 **없는 장소도 행을 남긴다**(`business_hours = NULL`) — 아니면 그런 장소를 매번 다시 묻는다.
- **실패는 보관하지 않는다** — 보관하면 영영 다시 안 받는다. "정보 없음"과 "조회 실패"는 다르다.
- 신규 테이블이라 `ddl-auto=update` 가 자동 생성한다. **마이그레이션 불필요.**
- 검증(실호출): 보관소가 찬 상태에서 목록 조회 → **detailIntro2 0회, 응답 0.02초, 영업시간 20/20**.
  일일 한도가 소진된 상태에서도 정상 응답했다.

### 한도에 대해 확인된 사실 (실측)
- **한도는 오퍼레이션별로 따로 집계된다.** `detailIntro2` 가 소진돼도 `areaBasedList2`/`searchKeyword2`/
  `locationBasedList2`/`detailCommon2` 는 정상 동작했다. 목록·검색이 영업시간 한도를 잠식하지 않는다.
- 초당 한도: **10 req/s 에서 0건**(안전). 25 req/s 를 지속하면 초과했다.
- 일일 한도 리셋: 날짜 단위(00:00 KST 기준). 9/9 소진분이 9/13에 복구된 것을 확인.
- 데이터 규모: 군산 333 + 전주 471 = **804곳**(우리 지역). 전북 2,425 / 전국 49,722.
  반경·키워드 검색은 지역 밖도 끌어오므로 보관 대상은 상한 없이 늘어난다(선택된 정책 = 조회된 건 모두 보관).

### 완충 장치(유지)
- 전용 회복성 인스턴스 `tourApiDetailIntroBulk`(서킷 + 레이트리미터 10 req/s) — 상세 화면과 격리.
- `tourapi.intro-concurrency=8`, `tourapi.intro-budget=4s`, 대기열 **LIFO**(`NewestFirstQueue`).
- 실패 시 예외 대신 null → 목록은 항상 200, 영업시간만 빈다. 상세(`/places/{id}`)도 동일하게
  영업시간 실패를 견딘다(예전엔 502로 죽었다).

## 🔴 배포 주의 — 러닝 테이블 재생성 필요 (2026-09-13)
`POST /runs` 구조 변경(multipart · 이미지 필수 · polyline 제거 · 출발/도착 좌표)에 따라
**운영 DB에서 `run` 테이블을 버려야** 합니다. `ddl-auto=update` 는 컬럼 추가만 하고
삭제·NULL 여부 변경은 하지 않으므로, 코드만 배포하면 스키마가 어긋나 저장이 실패합니다.
- 조치: **`migrate-run-start-end-coords.sql`(= `DROP TABLE run`)을 배포 직전에 실행.**
  앱이 기동하면서 엔티티대로 테이블을 새로 만듭니다.
- **기존 러닝 기록은 전부 사라집니다**(좌표가 없어 이관 불가 — 합의된 결정).
- `user_achievement` 는 건드리지 않아 이미 달성한 업적은 남습니다. 테스트 계정을 완전히
  초기화하려면 SQL 안의 주석 처리된 `DELETE FROM user_achievement;` 를 같이 실행하세요.
- **멱등키 `client_run_id`** 도 이 테이블에 포함됩니다(UNIQUE(user_id, client_run_id)).
  비로그인 러닝을 가입 후 몰아서 올리는 경로(클라이언트 캐시 방식으로 결정)에서 재시도가 잦은데,
  러닝 삭제 API가 없어 중복을 되돌릴 수 없고 누적 업적(완주 10회·100km)이 영구 오염되기 때문입니다.
- 업적 영향: `courseId`·`startedAt` 을 **선택 필드로 유지**했으므로 iOS가 보내면 21종 판정이 그대로 동작합니다.
  안 보내면 코스·지역·경유지·얼리버드 계열(9종)이 판정되지 않습니다.

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

## 🟢 비로그인 러닝 처리 방침 결정 (2026-09-14)
비로그인으로 달린 기록을 **기기에 캐시했다가 로그인/가입 후 업로드**하는 방식으로 결정.
서버는 비로그인 저장을 받지 않는 현재 구조를 유지한다(서버 변경 0).
- 탈락한 대안: 러닝 전 로그인 강제(진입 장벽) · 기록 폐기(첫 러닝 유실) ·
  익명 계정 후 병합(서버 구조 전복 + 병합 오류 시 타인 기록 혼입 위험, 기기 변경 시 어차피 복구 불가).
- 가능한 이유: `startedAt`/`finishedAt` 을 클라이언트가 보내므로 **나중에 올려도 달린 날짜가 유지**된다(실측 확인).
- 함께 도입: **멱등키 `clientRunId`** — 응답 유실 후 재전송으로 중복이 생기면 러닝 삭제 API가 없어
  되돌릴 수 없고, 누적 업적(완주 10회·100km)이 영구 오염된다. 로그인 여부와 무관하게 모든 저장에 권장.
- 클라이언트 구현 가이드: **`aidlc-docs/client-run-sync-guide.md`**(캐시 항목·순서·재시도·체크리스트).

## 기타 (기존 defer)
- 레이트리밋(공개 API 남용/스크래핑 방어 — CORS보다 본질적 방어책).
- at-rest 암호화(단일 EC2 로컬디스크 accepted risk), 위치정보(polyline) RDS 이관 시 우선 암호화.
- 러닝 수정/삭제, 완주율 계산, 통계/집계, 러닝 목록 페이징, polyline 크기 상한.
- 업적 진행률(%) 표시, 계정삭제 시 run/user_achievement cascade 정책(**+ 업로드 이미지 파일 정리 포함**).
- JaCoCo 커버리지, 자동 통합/부하/보안 스캔 도입.
