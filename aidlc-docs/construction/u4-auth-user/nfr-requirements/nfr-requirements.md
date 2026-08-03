# NFR Requirements — U4 인증·사용자 🔒

> 확정: Q1=A(nimbus Apple JWK) · Q2=A(레이트리밋 미적용) · Q3=A(refresh 해시저장) · Q4=A(소셜호출 Resilience4j) · Q5=A(at-rest accepted risk). U1-a/U2/U3 NFR 상속.

---

## 1. 확장성
- 단일 인스턴스(단일 EC2 코로케이션) 상속. 인증은 stateless JWT라 앱 수평확장에 유리(세션 없음).
- Refresh 상태만 Redis 공유 → 향후 다중 인스턴스에도 정합. 사용자 규모 소규모(초기).

## 2. 성능 (N2 기조 상속 — 하드 타깃 미설정)
- JWT 발급/검증은 인메모리 연산(빠름). Redis refresh 1회 조회.
- 로그인 시 소셜 외부 1콜(Kakao 조회 또는 Apple JWKS[캐시 HIT 시 0콜]) — 외부 지연이 지배적, 캐시로 완화.
- 하드 응답시간 타깃 미설정. 관측만(로깅).

## 3. 가용성·신뢰성 (Q4=A, U1-a 상속)
- **소셜 외부 호출 회복성**: Kakao 사용자 조회 / Apple JWKS 호출에 Resilience4j 적용 — connect 2s / read 3s + 재시도 1 + 서킷.
  - Kakao 조회 실패/무효 → **401**(인증 실패). 외부 장애/타임아웃 → 401 취급(로그인 불가, 데이터 손상 없음).
  - Apple **JWKS 캐시**(TTL, 키 로테이션 대비) — JWKS 일시 장애 시 캐시로 검증 지속.
- Refresh 회전 실패/Redis 미가용 시: 갱신 401(재로그인). 로그인 자체는 Redis 저장 실패 시 오류 반환(모니터).
- 인증 실패/접근 거부는 공통 `ApiResponse` 래퍼(401/403)로 일관 응답.

## 4. 보안 [Security Baseline] — U4 핵심
| 규칙 | U4 반영 | 상태 |
|---|---|---|
| SECURITY-01 (at-rest / in-transit) | **in-transit**: nginx TLS(배포). **at-rest**: 단일 EC2 로컬디스크 — 관리형 키 미도입 → **accepted risk(Q5=A)**, RDS 이관 시 활성 | 문서화된 수용 |
| SECURITY-03 (앱 로깅·민감정보 미노출) | 토큰·소셜 credential·JWT_SECRET 로그 금지(LogMaskingUtil 상속). 구조화 로깅 | 준수 |
| SECURITY-08 (deny-by-default) | SecurityConfig 기본 authenticated + 명시 화이트리스트(공개 사유 auth-classification) | 준수 |
| 인증/인가 | HS256 JWT 자체발급·검증, type 클레임 교차사용 차단, 본인 리소스 고정(users/me) | 준수 |
| 시크릿 관리 | `JWT_SECRET`·소셜 client 키 = env(코드/VCS 금지), U1-a `.env` 패턴 | 준수 |
| Refresh 저장 보호 | Redis에 **SHA-256 해시 저장**(Q3=A) — 유출 시 원문 비노출 | 준수(방어적) |
| 패스워드 | 소셜 전용 → 패스워드 없음 | N/A |
| 레이트리밋 | 이번 유닛 미적용(Q2=A) — 후속 백로그. refresh 회전/검증이 재발급 남용을 1차 억제 | 의도적 defer |

## 5. 유지보수·관측성
- 구조화 로깅 상속. 추가 관측 지점(민감정보 제외):
  - 로그인 성공/실패(provider, 신규가입 여부) — providerUserId/토큰은 미로깅 또는 마스킹.
  - refresh 갱신 실패(불일치/만료) 카운트, 로그아웃/계정삭제 이벤트.
  - 소셜 외부 호출 실패/서킷 오픈, Apple JWKS 캐시 미스/갱신.
- 유효성 위반(gender/height/weight/nickname) 400 응답 일관.

## 6. N/A (현 단계)
- 오토스케일링 / DR·failover / 컴플라이언스(GDPR 등) / 레이트리밋 인프라 — 현 단계 N/A 또는 defer.
- HTTP 보안 헤더(SECURITY-04): API-only(JSON, HTML 미서빙) → 해당 없음(N/A). CORS 정책은 필요 시 Infra/Code 단계에서 확인.
