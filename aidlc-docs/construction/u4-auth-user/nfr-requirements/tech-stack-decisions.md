# Tech Stack Decisions — U4 인증·사용자 🔒

> U1-a 스택(Spring Boot 4.0.6 / RestClient / Resilience4j / Redis / Jackson / Spring Validation / env 시크릿) 위에 U4/U1-b에 필요한 선택.

---

## 확정 기술 선택

| 관심사 | 선택 | 근거 |
|---|---|---|
| **JWT 발급/검증** | **jjwt (io.jsonwebtoken)** | U1-a tech-stack 확정. HS256 자체 발급·검증(Q5-FD=A) |
| **Apple identity token 검증** | **nimbus-jose-jwt** (Q1=A) | Apple JWKS fetch·kid 매칭·키 로테이션·RS256 서명검증을 견고히 처리 |
| **Kakao 검증** | **RestClient**(U1-a) 로 카카오 사용자 조회 API 호출(Q2-FD=A) | 별도 JWT 검증 불필요, access token 유효성=조회 성공 |
| **소셜 호출 회복성** | **Resilience4j**(U1-a) | connect2s/read3s + 재시도1 + 서킷 (Q4=A) |
| **Apple JWKS 캐시** | **Spring Cache(Redis 또는 로컬)** | JWKS TTL 캐시, 키 로테이션 대비 (Q4=A) |
| **Refresh Token 저장** | **Redis(Spring Data Redis)** — 값은 **SHA-256 해시** | 회전·1세션(Q6-FD=A), 해시 저장(Q3=A) |
| **비밀번호 해싱** | **N/A** | 소셜 전용, 패스워드 없음 |
| **검증** | **Spring Validation (Jakarta)** | 프로필 입력(gender/height/weight/nickname) |
| **보안 프레임워크** | **Spring Security**(기존) | deny-by-default + JwtAuthenticationFilter |

## 추가할 의존성 (build.gradle)
```gradle
// JWT (U1-b/U4 실사용) — Boot 4.0.x 호환 버전 Code Generation 시 고정
implementation 'io.jsonwebtoken:jjwt-api:0.12.x'
runtimeOnly   'io.jsonwebtoken:jjwt-impl:0.12.x'
runtimeOnly   'io.jsonwebtoken:jjwt-jackson:0.12.x'   // Jackson 직렬화(기존 Jackson 정합)

// Apple identity token JWK 검증
implementation 'com.nimbusds:nimbus-jose-jwt:9.x'
```
> **주의(호환성)**: Spring Boot **4.0.x** 기준 jjwt·nimbus 호환 버전을 Code Generation/Build&Test에서 확인·고정. (U1-a는 jjwt를 주석으로 남겨둠 → 이번에 실제 추가.)

## 시크릿/설정 (env 주입 — U1-a 패턴)
```properties
# JWT
jwt.secret=${JWT_SECRET}
jwt.access-token-ttl=24h
jwt.refresh-token-ttl=7d

# Kakao
oauth.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me

# Apple
oauth.apple.jwks-uri=https://appleid.apple.com/auth/keys
oauth.apple.issuer=https://appleid.apple.com
oauth.apple.audience=${APPLE_CLIENT_ID}   # 앱 번들ID/서비스ID
```
- 실제 값은 환경변수 / 로컬 `.env`(git 제외). 코드·VCS 저장 금지.
- `JWT_SECRET` 미설정 시 부팅 실패(안전 기본).

## 결정 요약(질문 답변 반영)
- Q1=A: **nimbus-jose-jwt**로 Apple JWK 검증.
- Q2=A: **레이트리밋 미적용**(후속 백로그).
- Q3=A: Redis refresh **SHA-256 해시 저장**.
- Q4=A: 소셜 호출 **Resilience4j** + Apple JWKS 캐시.
- Q5=A: at-rest 암호화 **accepted risk**(RDS 이관 시 활성) — 인프라 변경 없음.

## 유보 (이번 유닛 밖)
- 레이트리밋(Bucket4j 등) — 남용 관측 시 도입.
- at-rest 암호화(RDS/EBS) — 인프라 이관 시.
- Access Token 블랙리스트(즉시 무효화) — 현 stateless 정책상 범위 밖.
