# Infrastructure Design (Light) — U4 인증·사용자 🔒

> U1-a AWS 단일 EC2 코로케이션(app + MySQL + Redis) + U2 MySQL(ddl-auto=update) 상속. U4는 **users 테이블 + Redis refresh 키 + 소셜 egress(Kakao/Apple) + 신규 시크릿**만 추가. 새 매니지드 서비스 없음.

---

## 1. 논리 → 물리 매핑 (U4 신규분)

| 논리 컴포넌트 | 물리 인프라 | 비고 |
|---|---|---|
| `users` 테이블 | EC2 코로케이션 MySQL(:3306) | ddl-auto=update 자동 생성. `(provider, providerUserId)` 유니크 인덱스 |
| `RefreshTokenStore` | EC2 코로케이션 Redis(:6379) | 키 `refresh:{userId}` = SHA-256 해시, TTL 7d |
| `KakaoOAuthClient` | **아웃바운드 HTTPS 443** → kapi.kakao.com | 사용자 조회 API |
| `AppleJwksProvider`/`AppleOAuthClient` | **아웃바운드 HTTPS 443** → appleid.apple.com | JWKS fetch(캐시) |
| Apple JWKS 캐시 | Redis(:6379) 또는 인메모리 | TTL 6h (Code 단계 확정) |
| `JwtProvider`/`SecurityConfig`/필터 | 앱 프로세스 내(무상태) | 별도 인프라 없음 |

## 2. 스토리지
- **MySQL**: `users` 테이블 활성(ddl-auto=update). course/badge(U2/U3) 병존. 유니크 제약 `(provider, providerUserId)`.
- **Redis**: 기존 TourAPI 캐시(U3)와 **키 네임스페이스 공유**. refresh는 `refresh:*`, JWKS 캐시는 별도 prefix. localhost 바인딩.
- **at-rest 암호화**: 미적용(NFR Q5=A accepted risk) — 인프라 변경 없음. RDS 이관 시 활성.

## 3. 네트워킹
- **아웃바운드 443 확장**: 기존 TourAPI(apis.data.go.kr)에 더해 **kapi.kakao.com / appleid.apple.com** 허용 필요 — EC2 보안그룹 아웃바운드 443(전체 또는 도메인 대역) 확인.
- 인바운드: nginx(TLS 종단) → 앱 8080. **in-transit TLS는 nginx**(SECURITY-01 in-transit) — `deploy/nginx` 상속.
- Redis/MySQL localhost 전용(외부 미노출).

## 4. 시크릿·설정 (env 주입 — U1-a `.env` 패턴)
| 시크릿/설정 | 용도 | 필수 |
|---|---|---|
| `JWT_SECRET` | JWT HS256 서명키 | ✅ (미설정 시 부팅 실패) |
| `APPLE_CLIENT_ID` | Apple audience 검증(aud) | ✅ (Apple 로그인) |
| (Kakao user-info-uri) | application.properties 고정값 | — |
| (Apple jwks-uri/issuer) | application.properties 고정값 | — |
- 기존 `TOURAPI_SERVICE_KEY`, DB/Redis 접속정보 상속. 신규 키는 `.env.example`에 추가(값 없이 키만).
- Resilience4j 인스턴스 2종(`oauthKakaoUserInfo`, `oauthAppleJwks`) application.properties 추가.

## 5. 배포 영향
- 앱 JAR에 신규 의존성(jjwt, nimbus-jose-jwt) 포함. 기동 시 `users` 테이블 자동 생성.
- **배포 전 확인**: (1) `JWT_SECRET`·`APPLE_CLIENT_ID` env 설정, (2) 아웃바운드 443에 kakao/apple 도메인 도달성.

## 6. N/A / Deferred (상속)
- 매니지드 RDS/ElastiCache 승격, Flyway 마이그레이션, 중앙 로깅 → deferred(U1-a/U2/U3 동일).
- 레이트리밋 인프라, at-rest 암호화 → defer(NFR).
