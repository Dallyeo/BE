# NFR Requirements — U1-a 공개 기반

> 확정 답변: N1=A(소규모), N2=A(표준 회복성), N3=A(env 시크릿), N4=C(성능 하드타깃 미설정), N5=A(jjwt), N6=A(구조화 로깅).

---

## 1. 확장성 (Scalability) — N1=A
- **규모**: 소규모(동시 수십~수백, 전북 지역 대상). **단일 인스턴스**로 충분.
- **상태 비저장**: 애플리케이션은 stateless(세션 미사용, 토큰 기반). 향후 수평 확장 여지.
- **캐시**: Redis 공유 캐시로 다중 인스턴스 확장 시에도 캐시 일관.

## 2. 성능 (Performance) — N4=C
- **하드 타깃 미설정**(초기 기능 우선). 아래는 방향성만.
- TourAPI 부하/레이트리밋 완화를 위해 **Redis 캐시 TTL 30분+** 적용(BR-6).
- 상세 조합은 **병렬 호출**(U3, PlaceDetailAssembler)로 지연 최소화.
- 성능 지표(응답시간/히트율)는 **로깅으로 관측**만 하고, 정량 목표는 추후 필요 시 설정.

## 3. 가용성·신뢰성 (Availability & Reliability) — N2=A
- **외부 의존 격리**: TourAPI 장애가 앱 전체로 전파되지 않도록 Resilience4j 적용.
  - 타임아웃: connect **2s** / read **3s**
  - 재시도: **1회**(지수 백오프), 재시도 대상은 타임아웃/5xx
  - 서킷브레이커: 실패율 **50%** 초과 시 open, **10s** 후 half-open
  - 폴백: 실패 시 `EXTERNAL_API_ERROR`(502) 반환(부분 실패 명확화)
- 캐시 HIT 시 외부 장애와 무관하게 응답 가능.

## 4. 보안 (Security) — N3=A  [Security Baseline 적용]
- **시크릿 관리**: TourAPI `serviceKey`는 **환경변수 주입**(`${TOURAPI_SERVICE_KEY}`), 실제 값은 코드/문서/VCS에 저장 금지. 로컬은 `.env`(git 제외).
- **전송 보안**: 외부(TourAPI) 호출은 **HTTPS**.
- **로그 위생**: serviceKey·토큰·쿠키 등 민감정보 **로그 마스킹**(요청 URL 로깅 시 키 제거).
- **입력 검증**: 컨트롤러 파라미터 Bean Validation(좌표/반경/페이지 범위 등).
- **접근 통제(U1-a 한정)**: SecurityConfig **permitAll 껍데기** — 현재 🔒 엔드포인트 없음. deny-by-default 전환은 U1-b.
- **에러 노출 최소화**: 예외 메시지에 내부 스택/외부 원문 노출 금지(공통 실패 응답으로 감쌈).

## 5. 유지보수·관측성 (Maintainability & Observability) — N6=A
- **구조화 로깅**: 요청/외부 호출/실패를 구조화 로그로. 포함: 외부 API 실패, 캐시 히트율, **미매핑 카테고리 WARN**(BR-4.4), 좌표 파싱 제외 카운트(BR-3.4).
- **설정 외부화**: 타임아웃/TTL/키를 `application.properties` + env로.
- **테스트**: 외부 TourAPI는 목킹(WireMock 등)으로 단위/통합 테스트.

## 6. 미적용(N/A) 항목
- 재해복구/멀티리전, 오토스케일링 정책 — 소규모 단일 인스턴스라 현 단계 N/A.
- 컴플라이언스(PCI 등) — 결제 없음, N/A.
